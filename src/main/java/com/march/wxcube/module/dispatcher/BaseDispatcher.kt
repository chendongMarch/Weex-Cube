package com.march.wxcube.module.dispatcher

import android.support.v7.app.AppCompatActivity
import com.alibaba.fastjson.JSONObject
import com.march.wxcube.module.JsCallbackWrap

/**
 * CreateAt : 2018/6/6
 * Describe : 方法分发处理的 base 类
 *
 * @author chendong
 */
abstract class BaseDispatcher {

    interface Provider {
        fun activity(): AppCompatActivity
        fun doBySelf(method: String, params: JSONObject)
    }

    lateinit var mProvider: Provider

    companion object {
        // key
        const val KEY_SUCCESS = "success"
        const val KEY_FAIL = "fail"
        const val KEY_CANCEL = "cancel"
        const val KEY_MSG = "msg"
        const val KEY_URL = "url"
        const val KEY_LIST = "list"
        const val KEY_TAG = "tag"
        const val KEY_EVENT = "event"
        const val KEY_DATA = "data"
        const val KEY_DURATION = "duration"
        const val KEY_CONFIG = "config"
        const val KEY_RESULT = "result"
        const val KEY_CODE = "code"

    }

    open fun getAsyncMethods() = arrayOf<String>()

    abstract fun getMethods(): kotlin.Array<String>

    abstract fun dispatch(method: String, params: JSONObject, jsCallbackWrap: JsCallbackWrap)

    fun findAct(): AppCompatActivity {
        return mProvider.activity()
    }

    fun postJsResult(jsCallback: JsCallbackWrap, result: Pair<Boolean, String>) {
        jsCallback.invoke(mapOf(
                BaseDispatcher.KEY_SUCCESS to result.first,
                BaseDispatcher.KEY_MSG to result.second))
    }
}