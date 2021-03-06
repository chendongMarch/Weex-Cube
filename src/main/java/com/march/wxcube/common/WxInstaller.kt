package com.march.wxcube.common

import com.alibaba.android.bindingx.plugin.weex.BindingX
import com.march.wxcube.module.BridgeModule
import com.march.wxcube.widget.Container
import com.taobao.weex.WXSDKEngine
import com.taobao.weex.common.WXException

/**
 * CreateAt : 2018/6/27
 * Describe :
 *
 * @author chendong
 */
object WxInstaller {

    // 注册 bindingX
    fun registerBindingX() {
        try {
            BindingX.register()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 注册 Compnent
    fun registerComponent() {
        try {
            WXSDKEngine.registerComponent("container", Container::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 注册 module
    fun registerModule() {
        try {
            WXSDKEngine.registerModule("bridge", BridgeModule::class.java, true)
        } catch (e: WXException) {
            e.printStackTrace()
        }
    }




}