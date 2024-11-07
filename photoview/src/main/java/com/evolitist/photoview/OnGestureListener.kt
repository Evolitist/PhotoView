package com.evolitist.photoview

internal interface OnGestureListener {

    fun onDrag(isMultiTouch: Boolean, dx: Float, dy: Float)

    fun onFling(startX: Float, startY: Float, velocityX: Float, velocityY: Float)

    fun onScale(scaleFactor: Float, focusX: Float, focusY: Float)
}
