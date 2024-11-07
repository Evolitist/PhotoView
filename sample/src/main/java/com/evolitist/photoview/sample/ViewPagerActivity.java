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
package com.evolitist.photoview.sample;

import android.os.Bundle;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.evolitist.photoview.PhotoView;

public class ViewPagerActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pager);
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new SamplePagerAdapter());
    }

    static class SamplePagerAdapter extends RecyclerView.Adapter<SamplePageViewHolder> {

        private static final int[] sDrawables = {R.drawable.wallpaper, R.drawable.wallpaper, R.drawable.wallpaper,
            R.drawable.wallpaper, R.drawable.wallpaper, R.drawable.wallpaper};

        @Override
        public int getItemCount() {
            return sDrawables.length;
        }

        @NonNull
        @Override
        public SamplePageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new SamplePageViewHolder(new PhotoView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull SamplePageViewHolder holder, int position) {
            holder.bind(sDrawables[position]);
        }
    }

    static class SamplePageViewHolder extends RecyclerView.ViewHolder {

        public SamplePageViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void bind(int imageId) {
            Glide.with(itemView.getContext())
                    .load(imageId)
                    .into(((PhotoView) itemView));
        }
    }
}
