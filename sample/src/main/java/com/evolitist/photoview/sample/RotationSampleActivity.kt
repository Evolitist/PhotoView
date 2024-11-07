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
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.evolitist.photoview.PhotoView

class RotationSampleActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var photo: PhotoView
    private var rotating = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rotation_sample)
        photo = findViewById(R.id.iv_photo)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.rotation)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_rotate_10_right -> {
                    photo.setRotationBy(10f)
                    true
                }
                R.id.action_rotate_10_left -> {
                    photo.setRotationBy(-10f)
                    true
                }
                R.id.action_toggle_automatic_rotation -> {
                    toggleRotation()
                    true
                }
                R.id.action_reset_to_0 -> {
                    photo.setRotationTo(0f)
                    true
                }
                R.id.action_reset_to_90 -> {
                    photo.setRotationTo(90f)
                    true
                }
                R.id.action_reset_to_180 -> {
                    photo.setRotationTo(180f)
                    true
                }
                R.id.action_reset_to_270 -> {
                    photo.setRotationTo(270f)
                    true
                }
                else -> false
            }
        }
        photo.setImageResource(R.drawable.wallpaper)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }

    private fun toggleRotation() {
        if (rotating) {
            handler.removeCallbacksAndMessages(null)
        } else {
            rotateLoop()
        }
        rotating = !rotating
    }

    private fun rotateLoop() {
        handler.postDelayed({
            photo.setRotationBy(1f)
            rotateLoop()
        }, 15)
    }
}
