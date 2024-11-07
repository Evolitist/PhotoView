package com.evolitist.photoview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewParent
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.ImageView
import android.widget.OverScroller
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The component of [PhotoView] which does the work allowing for zooming, scaling, panning, etc.
 * It is made public in case you need to subclass something other than AppCompatImageView and still
 * gain the functionality that [PhotoView] offers
 */
@SuppressLint("ClickableViewAccessibility")
internal class PhotoViewAttacher(
    private val imageView: ImageView,
) : View.OnTouchListener, View.OnLayoutChangeListener {

    companion object {
        private const val DEFAULT_MAX_SCALE = 3.0f
        private const val DEFAULT_MIN_SCALE = 1.0f
        private const val DEFAULT_ZOOM_DURATION = 200

        private const val HORIZONTAL_EDGE_NONE = -1
        private const val HORIZONTAL_EDGE_LEFT = 0
        private const val HORIZONTAL_EDGE_RIGHT = 1
        private const val HORIZONTAL_EDGE_BOTH = 2
        private const val VERTICAL_EDGE_NONE = -1
        private const val VERTICAL_EDGE_TOP = 0
        private const val VERTICAL_EDGE_BOTTOM = 1
        private const val VERTICAL_EDGE_BOTH = 2
        private const val SINGLE_TOUCH = 1
    }

    // These are set so we don't keep allocating them on the heap
    private val _displayRect: RectF = RectF()
    private val baseMatrix: Matrix = Matrix()
    private val suppMatrix: Matrix = Matrix()
    private val matrixValues: FloatArray = FloatArray(9)

    private var allowParentInterceptOnEdge: Boolean = true
    private var blockParentIntercept: Boolean = false

    // Listeners
    private var photoTapListener: OnPhotoTapListener? = null
    private var outsidePhotoTapListener: OnOutsidePhotoTapListener? = null
    private var viewTapListener: OnViewTapListener? = null
    private var onClickListener: View.OnClickListener? = null
    private var longClickListener: View.OnLongClickListener? = null
    private var scaleChangeListener: OnScaleChangedListener? = null
    private var singleFlingListener: OnSingleFlingListener? = null
    private var onViewDragListener: OnViewDragListener? = null

    private var currentFlingRunnable: FlingRunnable? = null
    private var horizontalScrollEdge: Int = HORIZONTAL_EDGE_BOTH
    private var verticalScrollEdge: Int = VERTICAL_EDGE_BOTH
    private var baseRotation: Float = 0f

    private val drawMatrix: Matrix
        get() = imageMatrix.apply {
            set(baseMatrix)
            postConcat(suppMatrix)
        }

    val imageMatrix: Matrix = Matrix()

    val displayRect: RectF?
        get() {
            checkMatrixBounds()
            return getDisplayRect(drawMatrix)
        }

    var zoomInterpolator: Interpolator = AccelerateDecelerateInterpolator()
    var zoomTransitionDuration: Int = DEFAULT_ZOOM_DURATION

    var isDoubleTapEnabled: Boolean = true
        set(value) {
            field = value
            updateDoubleTapListener()
        }

    var isZoomable: Boolean = true
        set(zoomable) {
            field = zoomable
            update()
        }

    var minimumScale: Float = DEFAULT_MIN_SCALE
        set(minimumScale) {
            Util.checkZoomLevels(minimumScale, maximumScale)
            field = minimumScale
        }

    var maximumScale: Float = DEFAULT_MAX_SCALE
        set(maximumScale) {
            Util.checkZoomLevels(minimumScale, maximumScale)
            field = maximumScale
        }

    var scale: Float
        get() = hypot(getValue(suppMatrix, Matrix.MSCALE_X), getValue(suppMatrix, Matrix.MSKEW_Y))
        set(scale) {
            setScale(scale, false)
        }

    var scaleType = ImageView.ScaleType.FIT_CENTER
        set(scaleType) {
            if (Util.isSupportedScaleType(scaleType) && field != scaleType) {
                field = scaleType
                update()
            }
        }

    // Gesture Detectors
    private val gestureDetector: GestureDetector
    private val scaleDragDetector: CustomGestureDetector by lazy {
        CustomGestureDetector(imageView.context, onGestureListener)
    }
    private val onGestureListener = object : OnGestureListener {
        override fun onDrag(isMultiTouch: Boolean, dx: Float, dy: Float) {
            // Do not drag if we are already scaling
            if (scaleDragDetector.isScaling) return

            onViewDragListener?.onDrag(dx, dy)
            suppMatrix.postTranslate(dx, dy)
            checkAndDisplayMatrix()

            /*
             * Here we decide whether to let the ImageView's parent to start taking
             * over the touch event.
             *
             * First we check whether this function is enabled. We never want the
             * parent to take over if we're scaling. We then check the edge we're
             * on, and the direction of the scroll (i.e. if we're pulling against
             * the edge, aka 'overscrolling', let the parent take over).
             */
            val parent: ViewParent? = imageView.parent
            if (allowParentInterceptOnEdge && !scaleDragDetector.isScaling && !blockParentIntercept) {
                if (horizontalScrollEdge == HORIZONTAL_EDGE_BOTH ||
                    (horizontalScrollEdge == HORIZONTAL_EDGE_LEFT && dx >= 1f) ||
                    (horizontalScrollEdge == HORIZONTAL_EDGE_RIGHT && dx <= -1f) ||
                    (verticalScrollEdge == VERTICAL_EDGE_TOP && dy >= 1f) ||
                    (verticalScrollEdge == VERTICAL_EDGE_BOTTOM && dy <= -1f)
                ) {
                    if (!isMultiTouch) parent?.requestDisallowInterceptTouchEvent(false)
                }
            } else {
                parent?.requestDisallowInterceptTouchEvent(true)
            }
        }

        override fun onFling(startX: Float, startY: Float, velocityX: Float, velocityY: Float) {
            currentFlingRunnable = FlingRunnable(imageView.context).also {
                it.fling(
                    getImageViewWidth(imageView),
                    getImageViewHeight(imageView),
                    velocityX.toInt(),
                    velocityY.toInt(),
                )
                imageView.post(it)
            }
        }

        override fun onScale(scaleFactor: Float, focusX: Float, focusY: Float) {
            if (this@PhotoViewAttacher.scale < maximumScale || scaleFactor < 1f) {
                scaleChangeListener?.onScaleChange(scaleFactor, focusX, focusY)
                suppMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
                checkAndDisplayMatrix()
            }
        }
    }
    private val onDoubleTapListener = object : GestureDetector.OnDoubleTapListener {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            onClickListener?.onClick(imageView)
            val x = e.x
            val y = e.y
            viewTapListener?.onViewTap(imageView, x, y)
            displayRect?.let { rect ->
                // Check to see if the user tapped on the photo
                if (rect.contains(x, y)) {
                    val xResult = (x - rect.left) / rect.width()
                    val yResult = (y - rect.top) / rect.height()
                    photoTapListener?.onPhotoTap(imageView, xResult, yResult)
                    return true
                } else {
                    outsidePhotoTapListener?.onOutsidePhotoTap(imageView)
                }
            }
            return false
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            try {
                if (scale < maximumScale) {
                    setScale(maximumScale, e.x, e.y, true)
                } else {
                    setScale(minimumScale, e.x, e.y, true)
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                // Can sometimes happen when getX() and getY() are called
            }
            return true
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            // Wait for the confirmed onDoubleTap() instead
            return false
        }
    }

    init {
        imageView.setOnTouchListener(this)
        imageView.addOnLayoutChangeListener(this)
        // Create Gesture Detectors...
        gestureDetector = GestureDetector(
            imageView.context,
            object : GestureDetector.SimpleOnGestureListener() {
                // forward long click listener
                override fun onLongPress(e: MotionEvent) {
                    longClickListener?.onLongClick(imageView)
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float,
                ): Boolean {
                    e1 ?: return false
                    if (scale > DEFAULT_MIN_SCALE) {
                        return false
                    }
                    if (e1.pointerCount > SINGLE_TOUCH || e2.pointerCount > SINGLE_TOUCH) {
                        return false
                    }
                    return singleFlingListener?.onFling(e1, e2, velocityX, velocityY) ?: false
                }

                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    if (isDoubleTapEnabled) return false
                    onClickListener?.onClick(imageView)
                    val x = e.x
                    val y = e.y
                    viewTapListener?.onViewTap(imageView, x, y)
                    displayRect?.let { rect ->
                        // Check to see if the user tapped on the photo
                        if (rect.contains(x, y)) {
                            val xResult = (x - rect.left) / rect.width()
                            val yResult = (y - rect.top) / rect.height()
                            photoTapListener?.onPhotoTap(imageView, xResult, yResult)
                            return true
                        } else {
                            outsidePhotoTapListener?.onOutsidePhotoTap(imageView)
                        }
                    }
                    return false
                }
            },
        )
        gestureDetector.setOnDoubleTapListener(onDoubleTapListener)
    }

    override fun onLayoutChange(
        v: View,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int,
    ) {
        // Update our base matrix, as the bounds have changed
        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
            updateBaseMatrix(imageView.drawable)
        }
    }

    override fun onTouch(v: View, ev: MotionEvent): Boolean {
        var handled = false
        if (isZoomable && Util.hasDrawable((v as ImageView))) {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    val parent = v.getParent()
                    // First, disable the Parent from intercepting the touch event
                    parent?.requestDisallowInterceptTouchEvent(true)
                    // If we're flinging, and the user presses down, cancel fling
                    cancelFling()
                }
                // If the user has zoomed less than min scale, zoom back to min scale
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> if (scale < minimumScale) {
                    displayRect?.let { rect ->
                        v.post(AnimatedZoomRunnable(scale, minimumScale, rect.centerX(), rect.centerY()))
                    }
                } else if (scale > maximumScale) {
                    displayRect?.let { rect ->
                        v.post(AnimatedZoomRunnable(scale, maximumScale, rect.centerX(), rect.centerY()))
                    }
                }
            }
            // Try the Scale/Drag detector
            val wasScaling = scaleDragDetector.isScaling
            val wasDragging = scaleDragDetector.isDragging
            handled = scaleDragDetector.onTouchEvent(ev)
            val didntScale = !wasScaling && !scaleDragDetector.isScaling
            val didntDrag = !wasDragging && !scaleDragDetector.isDragging
            blockParentIntercept = didntScale && didntDrag
            // Check to see if the user double tapped
            if (gestureDetector.onTouchEvent(ev)) {
                handled = true
            }
        }
        return handled
    }

    fun setOnScaleChangeListener(onScaleChangeListener: OnScaleChangedListener?) {
        this.scaleChangeListener = onScaleChangeListener
    }

    fun setOnSingleFlingListener(onSingleFlingListener: OnSingleFlingListener?) {
        this.singleFlingListener = onSingleFlingListener
    }

    fun setDisplayMatrix(finalMatrix: Matrix): Boolean {
        if (imageView.drawable == null) {
            return false
        }
        suppMatrix.set(finalMatrix)
        checkAndDisplayMatrix()
        return true
    }

    fun setBaseRotation(degrees: Float) {
        baseRotation = degrees % 360
        update()
        setRotationBy(baseRotation)
        checkAndDisplayMatrix()
    }

    fun setRotationTo(degrees: Float) {
        suppMatrix.setRotate(degrees % 360)
        checkAndDisplayMatrix()
    }

    fun setRotationBy(degrees: Float) {
        suppMatrix.postRotate(degrees % 360)
        checkAndDisplayMatrix()
    }

    fun setAllowParentInterceptOnEdge(allow: Boolean) {
        allowParentInterceptOnEdge = allow
    }

    fun setScaleLevels(minimumScale: Float, maximumScale: Float) {
        Util.checkZoomLevels(minimumScale, maximumScale)
        this.minimumScale = minimumScale
        this.maximumScale = maximumScale
    }

    fun setOnLongClickListener(listener: View.OnLongClickListener?) {
        longClickListener = listener
    }

    fun setOnClickListener(listener: View.OnClickListener?) {
        onClickListener = listener
    }

    fun setOnPhotoTapListener(listener: OnPhotoTapListener?) {
        photoTapListener = listener
    }

    fun setOnOutsidePhotoTapListener(mOutsidePhotoTapListener: OnOutsidePhotoTapListener?) {
        this.outsidePhotoTapListener = mOutsidePhotoTapListener
    }

    fun setOnViewTapListener(listener: OnViewTapListener?) {
        viewTapListener = listener
    }

    fun setOnViewDragListener(listener: OnViewDragListener?) {
        onViewDragListener = listener
    }

    fun setScale(scale: Float, animate: Boolean) {
        setScale(scale, imageView.right / 2f, imageView.bottom / 2f, animate)
    }

    fun setScale(scale: Float, focalX: Float, focalY: Float, animate: Boolean) {
        // Check to see if the scale is within bounds
        require(scale in minimumScale..maximumScale) { "Scale must be within the range of minScale and maxScale" }
        if (animate) {
            imageView.post(AnimatedZoomRunnable(this.scale, scale, focalX, focalY))
        } else {
            suppMatrix.setScale(scale, scale, focalX, focalY)
            checkAndDisplayMatrix()
        }
    }

    fun update() {
        if (isZoomable) {
            // Update the base matrix using the current drawable
            updateBaseMatrix(imageView.drawable)
        } else {
            // Reset the Matrix...
            resetMatrix()
        }
    }

    /**
     * Get the display matrix
     *
     * @param matrix target matrix to copy to
     */
    fun getDisplayMatrix(matrix: Matrix) {
        matrix.set(drawMatrix)
    }

    /**
     * Get the current support matrix
     */
    fun getSuppMatrix(matrix: Matrix) {
        matrix.set(suppMatrix)
    }

    private fun updateDoubleTapListener() {
        if (isDoubleTapEnabled) {
            gestureDetector.setOnDoubleTapListener(onDoubleTapListener)
        } else {
            gestureDetector.setOnDoubleTapListener(null)
        }
    }

    /**
     * Helper method that 'unpacks' a Matrix and returns the required value
     *
     * @param matrix     Matrix to unpack
     * @param whichValue Which value from Matrix.M* to return
     * @return returned value
     */
    private fun getValue(matrix: Matrix, whichValue: Int): Float {
        matrix.getValues(matrixValues)
        return matrixValues[whichValue]
    }

    /**
     * Resets the Matrix back to FIT_CENTER, and then displays its contents
     */
    private fun resetMatrix() {
        suppMatrix.reset()
        setRotationBy(baseRotation)
        setImageViewMatrix(drawMatrix)
        checkMatrixBounds()
    }

    private fun setImageViewMatrix(matrix: Matrix) {
        imageView.imageMatrix = matrix
    }

    /**
     * Helper method that simply checks the Matrix, and then displays the result
     */
    private fun checkAndDisplayMatrix() {
        if (checkMatrixBounds()) {
            setImageViewMatrix(drawMatrix)
        }
    }

    /**
     * Helper method that maps the supplied Matrix to the current Drawable
     *
     * @param matrix - Matrix to map Drawable against
     * @return RectF - Displayed Rectangle
     */
    private fun getDisplayRect(matrix: Matrix): RectF? {
        return imageView.drawable?.let { d ->
            _displayRect[0f, 0f, d.intrinsicWidth.toFloat()] = d.intrinsicHeight.toFloat()
            matrix.mapRect(_displayRect)
            _displayRect
        }
    }

    /**
     * Calculate Matrix for FIT_CENTER
     *
     * @param drawable - Drawable being displayed
     */
    private fun updateBaseMatrix(drawable: Drawable?) {
        if (drawable == null) {
            return
        }
        val viewWidth = getImageViewWidth(imageView).toFloat()
        val viewHeight = getImageViewHeight(imageView).toFloat()
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        baseMatrix.reset()
        val widthScale = viewWidth / drawableWidth
        val heightScale = viewHeight / drawableHeight
        when (scaleType) {
            ImageView.ScaleType.CENTER -> {
                baseMatrix.postTranslate(
                    (viewWidth - drawableWidth) / 2f,
                    (viewHeight - drawableHeight) / 2f,
                )
            }
            ImageView.ScaleType.CENTER_CROP -> {
                val scale = max(widthScale.toDouble(), heightScale.toDouble()).toFloat()
                baseMatrix.postScale(scale, scale)
                baseMatrix.postTranslate(
                    (viewWidth - drawableWidth * scale) / 2f,
                    (viewHeight - drawableHeight * scale) / 2f,
                )
            }
            ImageView.ScaleType.CENTER_INSIDE -> {
                val scale = min(1.0, min(widthScale.toDouble(), heightScale.toDouble())).toFloat()
                baseMatrix.postScale(scale, scale)
                baseMatrix.postTranslate(
                    (viewWidth - drawableWidth * scale) / 2f,
                    (viewHeight - drawableHeight * scale) / 2f,
                )
            }
            else -> {
                var tempSrc = RectF(0f, 0f, drawableWidth.toFloat(), drawableHeight.toFloat())
                val tempDst = RectF(0f, 0f, viewWidth, viewHeight)
                if (baseRotation.toInt() % 180 != 0) {
                    tempSrc = RectF(0f, 0f, drawableHeight.toFloat(), drawableWidth.toFloat())
                }
                when (scaleType) {
                    ImageView.ScaleType.FIT_CENTER -> baseMatrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.CENTER)
                    ImageView.ScaleType.FIT_START -> baseMatrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.START)
                    ImageView.ScaleType.FIT_END -> baseMatrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.END)
                    ImageView.ScaleType.FIT_XY -> baseMatrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.FILL)
                    else -> Unit
                }
            }
        }
        resetMatrix()
    }

    private fun checkMatrixBounds(): Boolean {
        val rect = getDisplayRect(drawMatrix) ?: return false
        val width = rect.width()
        val height = rect.height()
        var deltaX = 0f
        var deltaY = 0f
        val viewWidth = getImageViewWidth(imageView)
        when {
            width <= viewWidth -> {
                deltaX = when (scaleType) {
                    ImageView.ScaleType.FIT_START -> -rect.left
                    ImageView.ScaleType.FIT_END -> viewWidth - width - rect.left
                    else -> (viewWidth - width) / 2 - rect.left
                }
                horizontalScrollEdge = HORIZONTAL_EDGE_BOTH
            }
            rect.left > 0 -> {
                horizontalScrollEdge = HORIZONTAL_EDGE_LEFT
                deltaX = -rect.left
            }
            rect.right < viewWidth -> {
                deltaX = viewWidth - rect.right
                horizontalScrollEdge = HORIZONTAL_EDGE_RIGHT
            }
            else -> {
                horizontalScrollEdge = HORIZONTAL_EDGE_NONE
            }
        }
        val viewHeight = getImageViewHeight(imageView)
        when {
            height <= viewHeight -> {
                deltaY = when (scaleType) {
                    ImageView.ScaleType.FIT_START -> -rect.top
                    ImageView.ScaleType.FIT_END -> viewHeight - height - rect.top
                    else -> (viewHeight - height) / 2 - rect.top
                }
                verticalScrollEdge = VERTICAL_EDGE_BOTH
            }
            rect.top > 0 -> {
                verticalScrollEdge = VERTICAL_EDGE_TOP
                deltaY = -rect.top
            }
            rect.bottom < viewHeight -> {
                verticalScrollEdge = VERTICAL_EDGE_BOTTOM
                deltaY = viewHeight - rect.bottom
            }
            else -> {
                verticalScrollEdge = VERTICAL_EDGE_NONE
            }
        }
        // Finally actually translate the matrix
        suppMatrix.postTranslate(deltaX, deltaY)
        return true
    }

    private fun getImageViewWidth(imageView: ImageView): Int {
        return imageView.width - imageView.paddingLeft - imageView.paddingRight
    }

    private fun getImageViewHeight(imageView: ImageView): Int {
        return imageView.height - imageView.paddingTop - imageView.paddingBottom
    }

    private fun cancelFling() {
        currentFlingRunnable?.cancelFling()
        currentFlingRunnable = null
    }

    private inner class AnimatedZoomRunnable(
        private val zoomStart: Float,
        private val zoomEnd: Float,
        private val focalX: Float,
        private val focalY: Float,
    ) : Runnable {

        private val startTime = System.currentTimeMillis()

        override fun run() {
            val t = interpolate()
            val scale = zoomStart + t * (zoomEnd - zoomStart)
            val deltaScale = scale / this@PhotoViewAttacher.scale
            onGestureListener.onScale(deltaScale, focalX, focalY)
            // We haven't hit our target scale yet, so post ourselves again
            if (t < 1f) {
                imageView.postOnAnimation(this)
            }
        }

        fun interpolate(): Float {
            val t = (1f * (System.currentTimeMillis() - startTime) / zoomTransitionDuration).coerceAtMost(1f)
            return zoomInterpolator.getInterpolation(t)
        }
    }

    private inner class FlingRunnable(context: Context?) : Runnable {
        private val scroller: OverScroller = OverScroller(context)
        private var currentX: Int = 0
        private var currentY: Int = 0

        fun cancelFling() {
            scroller.forceFinished(true)
        }

        fun fling(viewWidth: Int, viewHeight: Int, velocityX: Int, velocityY: Int) {
            val rect = this@PhotoViewAttacher.displayRect ?: return
            val startX = -rect.left.roundToInt()
            val (minX, maxX) = if (viewWidth < rect.width()) {
                0 to (rect.width() - viewWidth).roundToInt()
            } else {
                startX to startX
            }
            val startY = -rect.top.roundToInt()
            val (minY, maxY) = if (viewHeight < rect.height()) {
                0 to (rect.height() - viewHeight).roundToInt()
            } else {
                startY to startY
            }
            currentX = startX
            currentY = startY
            // If we actually can move, fling the scroller
            if (startX != maxX || startY != maxY) {
                scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0)
            }
        }

        override fun run() {
            if (scroller.isFinished) {
                return  // remaining post that should not be handled
            }
            if (scroller.computeScrollOffset()) {
                val newX = scroller.currX
                val newY = scroller.currY
                suppMatrix.postTranslate((currentX - newX).toFloat(), (currentY - newY).toFloat())
                checkAndDisplayMatrix()
                currentX = newX
                currentY = newY
                // Post On animation
                imageView.postOnAnimation(this)
            }
        }
    }
}
