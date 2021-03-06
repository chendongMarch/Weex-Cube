package com.march.wxcube

import com.march.wxcube.adapter.*
import com.march.wxcube.common.WxUtils
import com.march.wxcube.func.loader.JsCacheStrategy
import com.march.wxcube.func.loader.JsLoadStrategy
import com.march.wxcube.func.loader.JsPrepareStrategy
import com.march.wxcube.func.loader.WxJsLoader
import com.march.wxcube.func.router.WxRouter
import com.march.wxcube.func.update.WxUpdater


/**
 * CreateAt : 2018/4/20
 * Describe : 配置
 *
 * @author chendong
 */
class WxInitConfig {

    companion object {

        fun buildDebug(complete: WxInitConfig.() -> Unit): WxInitConfig {
            return WxInitConfig().apply(complete)
        }

        fun buildRelease(complete: WxInitConfig.() -> Unit): WxInitConfig {
            return WxInitConfig().apply {
                debug = false
                showDebugBtn = false
                logEnable = false
                fortest = false
            }.apply(complete)
        }
    }

    var smallImgHolder: Int = 0
    var largeImgHolder: Int = 0

    var https: Boolean = false // means force https
    var debug: Boolean = true // means debug mode
    var showDebugBtn = true // means show test btn
    var logEnable = true // means can log msg
    var fortest: Boolean = true // means show test msg
    var loadJsSafeMode = true // means only load local js resource
    var allImmersion = true // means all pages immersion

    // see WxJsLoader JsLoadStrategy/JsCacheStrategy/JsPrepareStrategy
    var jsLoadStrategy: Int = JsLoadStrategy.DEFAULT
    var jsCacheStrategy: Int = JsCacheStrategy.CACHE_MEMORY_DISK_BOTH
    var jsPrepareStrategy: Int = JsPrepareStrategy.PREPARE_ALL

    var wxCfgUrl: String = ""
    var onlineCfgUrl: String = ""
    var reqAuthority: String = ""
    var bundleAuthority: String = ""
    var bundlePathPrefix: String = ""
    var webAuthority: String = ""

    var wxModelAdapter: IWxModelAdapter = DefaultWxModelAdapter()
    var wxDebugAdapter: IWxDebugAdapter = DefaultWxDebugAdapter()
    var wxInitAdapter: IWxInitAdapter = DefaultWxInitAdapter()
    var wxPageAdapter: IWxPageAdapter = DefaultWxPageAdapter()
    var wxReportAdapter: IWxReportAdapter = DefaultWxReportAdapter()


    fun prepare(): WxInitConfig {
        CubeWx.mWxCfg = this

        // adapter
        CubeWx.mWxModelAdapter = wxModelAdapter
        CubeWx.mWxDebugAdapter = wxDebugAdapter
        CubeWx.mWxInitAdapter = wxInitAdapter
        CubeWx.mWxPageAdapter = wxPageAdapter
        CubeWx.mWxReportAdapter = wxReportAdapter
        //
        CubeWx.mRootCacheDir = WxUtils.makeRootCacheDir()
        //
        CubeWx.mWxJsLoader = WxJsLoader()
        CubeWx.mWxUpdater = WxUpdater()
        CubeWx.mWxRouter = WxRouter()
        return this
    }
}