package com.evolitist.photoview

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.VelocityTracker
import android.view.ViewConfiguration
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/**
 * Does a whole lot of gesture detecting.
 */
internal class CustomGestureDetector(
    context: Context,
    private val listener: OnGestureListener,
) {

    companion object {
        private const val INVALID_POINTER_ID = -1
    }

    private val velocityTracker: VelocityTracker = VelocityTracker.obtain()
    private val detector: ScaleGestureDetector
    private val touchSlop: Float
    private val minimumVelocity: Float

    private var activePointerId = INVALID_POINTER_ID
    private var activePointerIndex = 0

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    var isDragging: Boolean = false
        private set

    init {
        val configuration = ViewConfiguration.get(context)
        minimumVelocity = configuration.scaledMinimumFlingVelocity.toFloat()
        touchSlop = configuration.scaledTouchSlop.toFloat()

        detector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.OnScaleGestureListener {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val scaleFactor = detector.scaleFactor
                    if (!scaleFactor.isFinite()) return false
                    if (scaleFactor >= 0) {
                        this@CustomGestureDetector.listener.onScale(
                            scaleFactor,
                            detector.focusX,
                            detector.focusY,
                        )
                    }
                    return true
                }

                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = true

                override fun onScaleEnd(detector: ScaleGestureDetector) = Unit
            },
        )
    }

    private fun getActiveX(ev: MotionEvent): Float {
        return try {
            ev.getX(activePointerIndex)
        } catch (e: Exception) {
            ev.x
        }
    }

    private fun getActiveY(ev: MotionEvent): Float {
        return try {
            ev.getY(activePointerIndex)
        } catch (e: Exception) {
            ev.y
        }
    }

    val isScaling: Boolean
        get() = detector.isInProgress

    fun onTouchEvent(ev: MotionEvent): Boolean {
        try {
            detector.onTouchEvent(ev)
            return processTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            // Fix for support lib bug, happening when onDestroy is called
            return true
        }
    }

    private fun processTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(0)

                velocityTracker.addMovement(ev)

                lastTouchX = getActiveX(ev)
                lastTouchY = getActiveY(ev)
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val x = getActiveX(ev)
                val y = getActiveY(ev)
                val dx = x - lastTouchX
                val dy = y - lastTouchY

                if (!isDragging) {
                    // Use Pythagoras to see if drag length is larger than touch slop
                    isDragging = hypot(dx, dy) >= touchSlop
                }

                if (isDragging) {
                    listener.onDrag(ev.pointerCount > 1, dx, dy)
                    lastTouchX = x
                    lastTouchY = y

                    velocityTracker.addMovement(ev)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                activePointerId = INVALID_POINTER_ID
                // Clear Velocity Tracker
                velocityTracker.clear()
            }
            MotionEvent.ACTION_UP -> {
                activePointerId = INVALID_POINTER_ID
                if (isDragging) {
                    lastTouchX = getActiveX(ev)
                    lastTouchY = getActiveY(ev)

                    // Compute velocity within the last 1000ms
                    velocityTracker.addMovement(ev)
                    velocityTracker.computeCurrentVelocity(1000)

                    val vX = velocityTracker.xVelocity
                    val vY = velocityTracker.yVelocity

                    // If the velocity is greater than minVelocity, call listener
                    if (max(abs(vX), abs(vY)) >= minimumVelocity) {
                        listener.onFling(lastTouchX, lastTouchY, -vX, -vY)
                    }
                }

                // Clear Velocity Tracker
                velocityTracker.clear()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = Util.getPointerIndex(ev.action)
                val pointerId = ev.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    activePointerId = ev.getPointerId(newPointerIndex)
                    lastTouchX = ev.getX(newPointerIndex)
                    lastTouchY = ev.getY(newPointerIndex)
                }
            }
        }

        activePointerIndex = ev.findPointerIndex(if (activePointerId != INVALID_POINTER_ID) activePointerId else 0)
        return true
    }
}
