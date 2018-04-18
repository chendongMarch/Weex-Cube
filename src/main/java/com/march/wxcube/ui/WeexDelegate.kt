package com.march.wxcube.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.support.v4.app.Fragment
import android.view.View
import android.view.ViewGroup
import com.march.common.utils.LogUtils
import com.march.common.utils.StatusBarUtils
import com.march.common.utils.immersion.ImmersionStatusBarUtils
import com.march.webkit.IWebView
import com.march.webkit.sys.SysWebView
import com.march.webkit.x5.X5WebView
import com.march.wxcube.R
import com.march.wxcube.Weex
import com.march.wxcube.hub.DataHub
import com.march.wxcube.hub.EventHub

import com.march.wxcube.lifecycle.WeexLifeCycle
import com.march.wxcube.manager.BaseManager
import com.march.wxcube.model.WeexPage
import com.march.wxcube.module.EventModule
import com.taobao.weex.IWXRenderListener
import com.taobao.weex.WXSDKInstance
import java.lang.ref.WeakReference

import java.util.HashMap

/**
 * CreateAt : 2018/3/27
 * Describe : weex ui 代理
 *
 * @author chendong
 */
class WeexDelegate : WeexLifeCycle {

    companion object {
        val instanceDelegateMap: MutableMap<String, WeakReference<WeexDelegate>> = mutableMapOf()
    }

    private lateinit var weexRender: WeexRender
    private lateinit var weexInst: WXSDKInstance
    private lateinit var actContext: Activity

    var containerView: ViewGroup? = null // 容器 View
    private var weexPage: WeexPage? = null // 页面数据
    private var weexView: ViewGroup? = null // weex root view
    private val host: Any // 宿主
    private var managers = mutableMapOf<String, BaseManager>()

    // 为 Fragment 提供构造方法
    constructor(fragment: Fragment) {
        this.host = fragment
        this.weexPage = fragment.arguments.getParcelable(WeexPage.KEY_PAGE)
        init(fragment.activity)
    }

    // 为 Activity 提供构造方法
    constructor(activity: Activity) {
        this.host = activity
        this.containerView = activity.findViewById(R.id.weex_activity_root)
        this.weexPage = activity.intent.getParcelableExtra(WeexPage.KEY_PAGE)
        init(activity)
    }

    // 初始化方法
    private fun init(activity: Activity) {
        this.actContext = activity
        this.weexInst = WXSDKInstance(actContext)
        this.weexRender = WeexRender(actContext, weexInst, RenderListener())
        instanceDelegateMap[weexInst.instanceId] = WeakReference(this)
    }


    fun render() {
        val page = weexPage ?: return
        val realUrl = page.webUrl ?: return
        val opts = HashMap<String, Any>()
        // parse url
        if (!realUrl.isEmpty()) {
            val uri = Uri.parse(realUrl)
            val parameterNames = uri.queryParameterNames
            for (name in parameterNames) {
                opts[name] = uri.getQueryParameter(name)
            }
        }
        // parse data
        val data = DataHub.getData(realUrl)
        if (data != null) {
            opts["extraData"] = data
        }
        weexRender.render(page, opts)
    }

    fun putExtra(obj: BaseManager) {
        managers[obj.javaClass.simpleName] = obj
    }

    @Suppress("UNCHECKED_CAST")
    // getExtra(FragmentManager::class.java)
    fun <T> getExtra(clz: Class<T>): T? {
        val obj = managers[clz.simpleName]
        if (obj == null || obj.javaClass != clz) {
            return null
        }
        return obj as T
    }

    override fun close() {
        when (host) {
            is WeexActivity -> host.finish()
            is WeexFragment -> host.activity.finish()
            is WeexDialogFragment -> host.dismiss()
        }
    }

    override fun onCreate() {
        weexInst.onActivityCreate()
    }

    override fun onStart() {
        weexInst.onActivityStart()
    }

    override fun onResume() {
        weexInst.onActivityResume()
    }

    override fun onPause() {
        weexInst.onActivityPause()
    }

    override fun onStop() {
        weexInst.onActivityStop()
    }

    override fun onDestroy() {
        weexInst.onActivityDestroy()
        EventHub.clear(weexInst)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        weexInst.onActivityResult(requestCode, resultCode, data)
    }

    private var iWebView: IWebView? = null

    inner class RenderListener : IWXRenderListener {

        override fun onViewCreated(instance: WXSDKInstance, view: View) {
            weexView = view as ViewGroup
            containerView?.removeAllViews()
            containerView?.addView(view)
        }

        override fun onRenderSuccess(instance: WXSDKInstance, width: Int, height: Int) {
            for (manager in managers) {
                manager.value.onViewCreated()
            }
            LogUtils.e("onRenderSuccess")
        }

        override fun onRefreshSuccess(instance: WXSDKInstance, width: Int, height: Int) {

        }

        override fun onException(instance: WXSDKInstance, errCode: String, msg: String) {
            Weex.instance.weexService.onErrorReport(null, "code = $errCode, msg = $msg")
            if (weexPage?.webUrl != null) {
                if (iWebView == null) {
                    iWebView = X5WebView(actContext)
                }
                containerView?.removeAllViews()
                containerView?.addView(iWebView as View)
                iWebView?.loadPage(weexPage?.webUrl)
            }
        }
    }
}
