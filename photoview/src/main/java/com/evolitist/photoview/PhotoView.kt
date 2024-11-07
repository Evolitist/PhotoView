package com.evolitist.photoview

import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.animation.Interpolator
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.withStyledAttributes

/**
 * A zoomable ImageView. See [PhotoViewAttacher] for most of the details on how the zooming
 * is accomplished
 */
@Suppress("unused")
public class PhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private lateinit var attacher: PhotoViewAttacher
    private var pendingScaleType: ScaleType? = null
    private var pendingClickListener: OnClickListener? = null
    private var pendingLongClickListener: OnLongClickListener? = null

    public val displayRect: RectF?
        get() = attacher.displayRect

    public var scale: Float
        get() = attacher.scale
        set(value) {
            attacher.scale = value
        }
    public var minimumScale: Float
        get() = attacher.minimumScale
        set(value) {
            attacher.minimumScale = value
        }
    public var maximumScale: Float
        get() = attacher.maximumScale
        set(value) {
            attacher.maximumScale = value
        }
    public var isZoomable: Boolean
        get() = attacher.isZoomable
        set(value) {
            attacher.isZoomable = value
        }
    public var isDoubleTapEnabled: Boolean
        get() = attacher.isDoubleTapEnabled
        set(value) {
            attacher.isDoubleTapEnabled = value
        }
    public var zoomTransitionDuration: Int
        get() = attacher.zoomTransitionDuration
        set(value) {
            attacher.zoomTransitionDuration = value
        }
    public var zoomInterpolator: Interpolator
        get() = attacher.zoomInterpolator
        set(value) {
            attacher.zoomInterpolator = value
        }

    init {
        // We always pose as a Matrix scale type, though we can change to another scale type via the attacher
        super.setScaleType(ScaleType.MATRIX)
        //apply the previously applied scale type & click listeners
        attacher = PhotoViewAttacher(this)
        pendingScaleType?.let {
            scaleType = it
            pendingScaleType = null
        }
        pendingClickListener?.let {
            setOnClickListener(it)
            pendingClickListener = null
        }
        pendingLongClickListener?.let {
            setOnLongClickListener(it)
            pendingLongClickListener = null
        }
        context.withStyledAttributes(
            set = attrs,
            attrs = R.styleable.PhotoView,
            defStyleAttr = defStyleAttr,
        ) {
            attacher.isDoubleTapEnabled = getBoolean(R.styleable.PhotoView_doubleTapEnabled, true)
        }
    }

    override fun getImageMatrix(): Matrix = attacher.imageMatrix

    override fun getScaleType(): ScaleType = attacher.scaleType
    override fun setScaleType(scaleType: ScaleType) {
        if (!::attacher.isInitialized) {
            pendingScaleType = scaleType
        } else {
            attacher.scaleType = scaleType
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        if (!::attacher.isInitialized) {
            pendingClickListener = l
        } else {
            attacher.setOnClickListener(l)
        }
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        if (!::attacher.isInitialized) {
            pendingLongClickListener = l
        } else {
            attacher.setOnLongClickListener(l)
        }
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        // setImageBitmap calls through to this method
        if (::attacher.isInitialized) {
            attacher.update()
        }
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        if (::attacher.isInitialized) {
            attacher.update()
        }
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        if (::attacher.isInitialized) {
            attacher.update()
        }
    }

    override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
        val changed = super.setFrame(l, t, r, b)
        if (changed && ::attacher.isInitialized) {
            attacher.update()
        }
        return changed
    }

    public fun setBaseRotation(rotationDegree: Float) {
        attacher.setBaseRotation(rotationDegree)
    }

    public fun setRotationTo(rotationDegree: Float) {
        attacher.setRotationTo(rotationDegree)
    }

    public fun setRotationBy(rotationDegree: Float) {
        attacher.setRotationBy(rotationDegree)
    }

    public fun getDisplayMatrix(matrix: Matrix) {
        attacher.getDisplayMatrix(matrix)
    }

    public fun setDisplayMatrix(finalRectangle: Matrix): Boolean {
        return attacher.setDisplayMatrix(finalRectangle)
    }

    public fun getSuppMatrix(matrix: Matrix) {
        attacher.getSuppMatrix(matrix)
    }

    public fun setSuppMatrix(matrix: Matrix): Boolean {
        return attacher.setDisplayMatrix(matrix)
    }

    public fun setAllowParentInterceptOnEdge(allow: Boolean) {
        attacher.setAllowParentInterceptOnEdge(allow)
    }

    public fun setScaleLevels(minimumScale: Float, maximumScale: Float) {
        attacher.setScaleLevels(minimumScale, maximumScale)
    }

    public fun setOnPhotoTapListener(listener: OnPhotoTapListener?) {
        attacher.setOnPhotoTapListener(listener)
    }

    public fun setOnOutsidePhotoTapListener(listener: OnOutsidePhotoTapListener?) {
        attacher.setOnOutsidePhotoTapListener(listener)
    }

    public fun setOnViewTapListener(listener: OnViewTapListener?) {
        attacher.setOnViewTapListener(listener)
    }

    public fun setOnViewDragListener(listener: OnViewDragListener?) {
        attacher.setOnViewDragListener(listener)
    }

    public fun setScale(scale: Float, animate: Boolean) {
        attacher.setScale(scale, animate)
    }

    public fun setScale(scale: Float, focalX: Float, focalY: Float, animate: Boolean) {
        attacher.setScale(scale, focalX, focalY, animate)
    }

    public fun setOnScaleChangeListener(onScaleChangedListener: OnScaleChangedListener?) {
        attacher.setOnScaleChangeListener(onScaleChangedListener)
    }

    public fun setOnSingleFlingListener(onSingleFlingListener: OnSingleFlingListener?) {
        attacher.setOnSingleFlingListener(onSingleFlingListener)
    }
}
