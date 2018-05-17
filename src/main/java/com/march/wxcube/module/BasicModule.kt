package com.march.wxcube.module

import android.content.Intent
import android.support.v4.app.Fragment
import android.text.TextUtils
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.march.common.utils.LogUtils
import com.march.webkit.WebKit
import com.march.wxcube.Weex
import com.march.wxcube.manager.ManagerRegistry
import com.march.wxcube.model.DialogConfig
import com.march.wxcube.model.FragmentConfig
import com.march.wxcube.ui.FragmentLoader
import com.march.wxcube.ui.WebActivity
import com.march.wxcube.ui.WeexFragment
import com.taobao.weex.annotation.JSMethod
import com.taobao.weex.bridge.JSCallback
import com.taobao.weex.common.WXModule


/**
 * CreateAt : 2018/3/28
 * Describe : 基础 module
 *
 * @author chendong
 */
class BasicModule : WXModule() {

    companion object {
        const val KEY = "cube-basic"
    }

    /**
     * 读取 instantId
     */
    @JSMethod(uiThread = true)
    fun readInstanceId(jsCallback: JSCallback) {
        jsCallback.invoke(mWXSDKInstance.instanceId)
    }

    /**
     * 针对某个 url 存数据
     */
    @JSMethod(uiThread = true)
    fun putExtraData(url: String, data: JSONObject) {
        ManagerRegistry.DATA.putData(url, data)
    }

    /**
     * 关闭页面，dialog 会 dismiss
     */
    @JSMethod(uiThread = true)
    fun close() {
        val delegate = weexDelegate ?: return
        delegate.close()
    }

    /**
     * 打开页面
     */
    @JSMethod(uiThread = true)
    fun openUrl(webUrl: String) {
        val ctx = context ?: return
        Weex.getInst().mWeexRouter.openUrl(ctx, webUrl)
    }

    /**
     * 打开弹窗
     */
    @JSMethod(uiThread = true)
    fun openDialog(webUrl: String, params: JSONObject) {
        val act = activity ?: return
        val config = jsonObj2Obj(params, DialogConfig::class.java)
        Weex.getInst().mWeexRouter.openDialog(act, webUrl, config)
    }

    /**
     * 打开 web 界面
     */
    @JSMethod(uiThread = true)
    fun openWeb(webUrl: String) {
        val act = activity ?: return
        val intent = Intent(act, WebActivity::class.java)
        intent.putExtra(WebKit.KEY_URL, ManagerRegistry.ENV.safeUrl(webUrl))
        act.startActivity(intent)
    }

    /**
     * 加载 tab
     */
    @JSMethod(uiThread = true)
    fun loadTabs(array: JSONArray) {
        LogUtils.e("loadTabPages")
        val weexAct = weexActivity ?: return
        val configs = jsonArray2List(array, FragmentConfig::class.java)
        weexAct.weexDelegate.mFragmentLoader = FragmentLoader(weexAct.supportFragmentManager,
                configs, object : FragmentLoader.FragmentHandler {
            override fun containerIdFinder(): () -> Int {
                return {
                    val view = findView { it.tag == "container" }
                    view?.id ?: -1
                }
            }

            override fun makeFragment(tag: String): Fragment? {
                val config = configs.firstOrNull { it.tag.equals(tag) }
                if (config != null && !TextUtils.isEmpty(config.url)) {
                    val page = Weex.getInst().mWeexRouter.findPage(config.url!!) ?: return null
                    return WeexFragment.newInstance(page)
                }
                return null
            }
        })
    }

    /**
     * 显示 tab
     * @param tag tab 对应的 tag
     */
    @JSMethod(uiThread = true)
    fun showTab(tag: String) {
        val weexAct = weexActivity ?: return
        val loader = weexAct.weexDelegate.mFragmentLoader ?: return
        loader.showFragment(tag)
    }


    /**
     * 注册接受某事件
     * const event = weex.requireModule('cube-event')
     * event.registerEvent('myEvent')
     * globalEvent.addEventListener('myEvent', (params) => {});
     */
    @JSMethod(uiThread = true)
    fun registerEvent(key: String?) {
        ManagerRegistry.EVENT.registerEvent(key, instantId)
    }


    /**
     * 发送事件
     * const event = weex.requireModule('cube-event')
     * event.post('myEvent',{isOk:true});
     */
    @JSMethod(uiThread = true)
    fun postEvent(key: String, params: Map<String, Any>) {
        ManagerRegistry.EVENT.postEvent(key, params)
    }

}
