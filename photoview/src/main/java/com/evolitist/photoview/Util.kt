package com.evolitist.photoview

import android.view.MotionEvent
import android.widget.ImageView

internal object Util {
    fun checkZoomLevels(minZoom: Float, maxZoom: Float) {
        require(minZoom < maxZoom) {
            "Minimum zoom has to be less than maximum zoom. " +
                "Call setMinimumZoom() or setMaximumZoom() with a more appropriate value"
        }
    }

    fun hasDrawable(imageView: ImageView): Boolean {
        return imageView.drawable != null
    }

    fun isSupportedScaleType(scaleType: ImageView.ScaleType?): Boolean {
        if (scaleType == null) {
            return false
        }
        require(scaleType != ImageView.ScaleType.MATRIX) {
            "Matrix scale type is not supported"
        }
        return true
    }

    fun getPointerIndex(action: Int): Int {
        return (action and MotionEvent.ACTION_POINTER_INDEX_MASK) shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
    }
}
