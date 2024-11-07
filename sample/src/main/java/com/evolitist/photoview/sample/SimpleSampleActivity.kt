/*
 Copyright 2011, 2012 Chris Banes.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.evolitist.photoview.sample

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.evolitist.photoview.OnPhotoTapListener
import com.evolitist.photoview.OnSingleFlingListener
import com.evolitist.photoview.PhotoView
import java.util.Random

class SimpleSampleActivity : AppCompatActivity() {

    private var currentToast: Toast? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_sample)

        val photoView = findViewById<PhotoView>(R.id.iv_photo)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = "Simple Sample"
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_zoom_toggle -> {
                    photoView.isZoomable = !photoView.isZoomable
                    item.setTitle(if (photoView.isZoomable) R.string.menu_zoom_disable else R.string.menu_zoom_enable)
                    true
                }
                R.id.menu_scale_fit_center -> {
                    photoView.scaleType = ImageView.ScaleType.CENTER
                    true
                }
                R.id.menu_scale_fit_start -> {
                    photoView.scaleType = ImageView.ScaleType.FIT_START
                    true
                }
                R.id.menu_scale_fit_end -> {
                    photoView.scaleType = ImageView.ScaleType.FIT_END
                    true
                }
                R.id.menu_scale_fit_xy -> {
                    photoView.scaleType = ImageView.ScaleType.FIT_XY
                    true
                }
                R.id.menu_scale_scale_center -> {
                    photoView.scaleType = ImageView.ScaleType.CENTER
                    true
                }
                R.id.menu_scale_scale_center_crop -> {
                    photoView.scaleType = ImageView.ScaleType.CENTER_CROP
                    true
                }
                R.id.menu_scale_scale_center_inside -> {
                    photoView.scaleType = ImageView.ScaleType.CENTER_INSIDE
                    true
                }
                R.id.menu_scale_random_animate, R.id.menu_scale_random -> {
                    val r = Random()

                    val minScale = photoView.minimumScale
                    val maxScale = photoView.maximumScale
                    val randomScale = minScale + (r.nextFloat() * (maxScale - minScale))
                    photoView.setScale(randomScale, item.itemId == R.id.menu_scale_random_animate)

                    showToast(String.format(SCALE_TOAST_STRING, randomScale))

                    true
                }
                else -> false
            }
        }

        val bitmap = ContextCompat.getDrawable(this, R.drawable.wallpaper)
        photoView.setImageDrawable(bitmap)

        // Lets attach some listeners, not required though!
        photoView.setOnPhotoTapListener(PhotoTapListener())
        photoView.setOnSingleFlingListener(SingleFlingListener())
    }

    private inner class PhotoTapListener : OnPhotoTapListener {
        override fun onPhotoTap(view: ImageView, x: Float, y: Float) {
            val xPercentage = x * 100f
            val yPercentage = y * 100f

            showToast(
                String.format(PHOTO_TAP_TOAST_STRING, xPercentage, yPercentage, view.id)
            )
        }
    }

    private fun showToast(text: CharSequence) {
        currentToast?.cancel()
        currentToast = Toast.makeText(this@SimpleSampleActivity, text, Toast.LENGTH_SHORT).also {
            it.show()
        }
    }

    private class SingleFlingListener : OnSingleFlingListener {
        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            Log.d("PhotoView", String.format(FLING_LOG_STRING, velocityX, velocityY))
            return true
        }
    }

    companion object {
        const val PHOTO_TAP_TOAST_STRING: String = "Photo Tap! X: %.2f %% Y:%.2f %% ID: %d"
        const val SCALE_TOAST_STRING: String = "Scaled to: %.2ff"
        const val FLING_LOG_STRING: String = "Fling velocityX: %.2f, velocityY: %.2f"
    }
}
