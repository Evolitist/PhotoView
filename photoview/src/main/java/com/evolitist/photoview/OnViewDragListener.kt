package com.evolitist.photoview

/**
 * Interface definition for a callback to be invoked when the photo is experiencing a drag event
 */
public fun interface OnViewDragListener {

    /**
     * Callback for when the photo is experiencing a drag event. This cannot be invoked when the
     * user is scaling.
     *
     * @param dx The change of the coordinates in the x-direction
     * @param dy The change of the coordinates in the y-direction
     */
    public fun onDrag(dx: Float, dy: Float)
}
