package com.march.wxcube

import android.content.Context
import android.os.Build
import android.util.LruCache
import com.march.wxcube.common.DiskLruCache
import com.march.wxcube.common.memory
import com.march.wxcube.manager.ManagerRegistry

import com.march.wxcube.model.WeexPage
import com.taobao.weex.utils.WXFileUtils
import java.io.File

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CreateAt : 2018/3/27
 * Describe : 对 page 对应的 template 做缓存，加速访问，防止频繁 IO
 *
 * @author chendong
 */
class WeexJsLoader(context: Context, jsLoadStrategy: Int, jsCacheStrategy: Int) : UpdateHandler {

    companion object {
        private val TAG = WeexJsLoader::class.java.simpleName!!
        const val CACHE_DIR = "weex-js-disk-cache"
        const val DISK_MAX_SIZE = 20 * 1024 * 1024L
    }

    private var fromWhere: String = ""
    // 线程池
    private val mService: ExecutorService = Executors.newFixedThreadPool(1)
    // 加载策略
    private val mJsLoadStrategy = jsLoadStrategy
    // 缓存策略
    private val mJsCacheStrategy = jsCacheStrategy
    // 内存缓存
    private val mJsMemoryCache = JsMemoryCache(context.memory(.3f))
    // 文件缓存
    private val mJsFileCache = JsFileCache(Weex.getInst().makeCacheDir(CACHE_DIR), DISK_MAX_SIZE)

    override fun updateWeexPages(context: Context, weexPages: List<WeexPage>?) {
        if (mJsCacheStrategy == JsCacheStrategy.PREPARE_ALL) {
            weexPages?.forEach { getTemplateAsync(context, it) {} }
        }
    }

    fun getTemplateAsync(context: Context, loadStrategy: Int, cacheStrategy: Int, page: WeexPage?, consumer: (String?) -> Unit) {
        if (page == null) {
            return
        }
        val publishFunc: (String?) -> Unit = {
            consumer(it)
            if (cacheStrategy != JsCacheStrategy.NO_CACHE) {
                mJsMemoryCache.put(page, it)
            }
        }
        val runnable = if (loadStrategy != JsLoadStrategy.DEFAULT) {
            makeJsLoader(loadStrategy, context, page)
        } else {
            {
                var template: String? = ""
                if (template.isNullOrBlank()) {
                    template = makeJsLoader(JsLoadStrategy.CACHE_FIRST, context, page)()
                }
                if (template.isNullOrBlank() && !page.assetsJs.isNullOrBlank()) {
                    template = makeJsLoader(JsLoadStrategy.ASSETS_FIRST, context, page)()
                }
                if (template.isNullOrBlank() && !page.localJs.isNullOrBlank()) {
                    template = makeJsLoader(JsLoadStrategy.FILE_FIRST, context, page)()
                }
                if (template.isNullOrBlank() && !page.remoteJs.isNullOrBlank()) {
                    template = makeJsLoader(JsLoadStrategy.NET_FIRST, context, page)()
                }
                template
            }
        }
        mService.execute {
            val template = runnable.invoke()
            Weex.getInst().mWeexInjector.onLog(TAG, "JS加载${page.pageName} cache[${mJsMemoryCache.size()}] $fromWhere")
            publishFunc(template)
        }
    }

    fun getTemplateAsync(context: Context, page: WeexPage?, consumer: (String?) -> Unit) {
        getTemplateAsync(context, mJsLoadStrategy, mJsCacheStrategy, page, consumer)
    }

    fun clearCache() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mJsMemoryCache.trimToSize(-1)
        }
    }

    private fun downloadJs(page: WeexPage): String? {
        val url = page.remoteJs ?: return null
        val http = ManagerRegistry.HTTP
        val wxRequest = http.makeWxRequest(url = url, from = "download-js")
        val resp = http.requestSync(wxRequest, false)
        if (page.localJs != null && resp.data != null) {
            mJsFileCache.write(page.localJs!!, resp.data)
        }
        return resp.data
    }

    // 加载函数
    private fun makeJsLoader(type: Int, context: Context, page: WeexPage): () -> String? {
        return when (type) {
            JsLoadStrategy.CACHE_FIRST -> {
                {
                    fromWhere = "缓存"
                    mJsMemoryCache.get(page)
                }
            }
            JsLoadStrategy.ASSETS_FIRST -> {
                {
                    fromWhere = "assets"
                    WXFileUtils.loadAsset(page.assetsJs, context)
                }
            }
            JsLoadStrategy.FILE_FIRST -> {
                {
                    fromWhere = "文件"
                    page.localJs?.let { mJsFileCache.read(it) }
                }
            }
            JsLoadStrategy.NET_FIRST -> {
                {
                    fromWhere = "网络"
                    downloadJs(page)
                }
            }
            else -> {
                { "" }
            }
        }
    }
}

// js 内存缓存
class JsMemoryCache(maxSize: Int) : LruCache<WeexPage, String>(maxSize) {
    override fun sizeOf(key: WeexPage, value: String): Int {
        return value.length
    }
}

// js 文件缓存
class JsFileCache(dir: File, maxSize: Long) : DiskLruCache(dir, maxSize)

// 加载策略
object JsLoadStrategy {
    const val NET_FIRST = 0 // 只加载网络
    const val FILE_FIRST = 1 // 只加载文件
    const val ASSETS_FIRST = 2 // 只加载 assets
    const val CACHE_FIRST = 3 // 只加载缓存
    const val DEFAULT = 4 // 默认 缓存、文件、assets、网络 一次检查
}

// 缓存策略
object JsCacheStrategy {
    const val PREPARE_ALL = 0 // 提前准备所有的js到缓存中
    const val LAZY_LOAD = 2 // 使用时才加载
    const val NO_CACHE = 3 // 不缓存
}
