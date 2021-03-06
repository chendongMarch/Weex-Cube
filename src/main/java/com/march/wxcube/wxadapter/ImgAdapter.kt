package com.march.wxcube.wxadapter

import android.graphics.Bitmap
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.march.wxcube.CubeWx
import com.march.wxcube.manager.ManagerRegistry
import com.taobao.weex.adapter.IWXImgLoaderAdapter
import com.taobao.weex.adapter.URIAdapter
import com.taobao.weex.common.WXImageStrategy
import com.taobao.weex.dom.WXImageQuality

/**
 * CreateAt : 2018/3/26
 * Describe : 图片加载
 *
 * @author chendong
 */
class ImgAdapter : IWXImgLoaderAdapter {

    override fun setImage(url: String?, view: ImageView?, quality: WXImageQuality?, strategy: WXImageStrategy?) {
        if (view != null && url != null) {
            val parseUrl = UriWriter.rewrite(url, URIAdapter.IMAGE)
            if (url.endsWith("gif")) {
                loadGif(parseUrl, view, strategy)
            } else {
                loadImg(parseUrl, view, strategy)
            }
        } else {
            strategy?.imageListener?.onImageFinish(url, view, false, null)
        }
    }


    private fun loadImg(url: String, view: ImageView, strategy: WXImageStrategy?) {
        val options = RequestOptions()
        val width = view.measuredWidth
        val height = view.measuredHeight
        if (width > 0 && height > 0) {
            options.override((width *1.5).toInt(), (height*1.5).toInt())
            if (width > 300 && CubeWx.mWxCfg.largeImgHolder > 0) {
                options.placeholder(CubeWx.mWxCfg.largeImgHolder).error(CubeWx.mWxCfg.largeImgHolder)
            } else if (CubeWx.mWxCfg.smallImgHolder > 0) {
                options.placeholder(CubeWx.mWxCfg.smallImgHolder).error(CubeWx.mWxCfg.smallImgHolder)
            }
        }
        val request = Glide.with(view.context).asBitmap().apply(options)
        val res = ManagerRegistry.ResMapping.mapUrlRes(url)
        if (res > 0) {
            request.load(res)
        } else {
            request.load(url)
        }.listener(RequestBitmapListenerImpl(url, view, strategy)).into(view)
    }

    private fun loadGif(url: String, view: ImageView, strategy: WXImageStrategy?) {
        Glide.with(view.context).load(url).into(view)
    }


    class RequestBitmapListenerImpl(
            private val url: String?,
            private val view: ImageView?,
            private val strategy: WXImageStrategy?) : RequestListener<Bitmap> {
        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
            strategy?.imageListener?.onImageFinish(url, view, false, null)
            view ?: return false
            if (view.measuredWidth > 0 && view.measuredHeight > 0) {
                if (view.measuredWidth > 300 && CubeWx.mWxCfg.largeImgHolder > 0) {
                    setImageResource(view, CubeWx.mWxCfg.largeImgHolder)
                } else if (CubeWx.mWxCfg.smallImgHolder > 0) {
                    setImageResource(view, CubeWx.mWxCfg.smallImgHolder)
                }
            }
            return false
        }

        override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
            view?.setImageBitmap(resource)
            strategy?.imageListener?.onImageFinish(url, view, true, null)
            return false
        }

        fun setImageResource(imageView: ImageView, res: Int) {
            try {
                System.gc()
                imageView.setImageResource(res)
            } catch (ignore: Exception) {
            }
        }

    }


}
