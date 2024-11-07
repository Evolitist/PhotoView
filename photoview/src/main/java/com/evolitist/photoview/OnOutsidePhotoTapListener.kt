package com.evolitist.photoview

import android.widget.ImageView

/**
 * Callback when the user tapped outside the photo
 */
public fun interface OnOutsidePhotoTapListener {

    /**
     * The outside of the photo has been tapped
     */
    public fun onOutsidePhotoTap(imageView: ImageView)
}
