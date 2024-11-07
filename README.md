# PhotoView
PhotoView aims to help produce an easily usable implementation of a zooming Android ImageView.

[![](https://jitpack.io/v/Evolitist/PhotoView.svg)](https://jitpack.io/#Evolitist/PhotoView)

[![](https://user-images.githubusercontent.com/12352397/85141529-94648e80-b24f-11ea-9a14-a845fb43b181.gif)

## Dependency

Add this in your root `settings.gradle.kts` file:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven(url = "https://jitpack.io")
    }
}
```

Then, add the library to your module `build.gradle.kts`
```gradle
dependencies {
    implementation("com.github.evolitist:PhotoView:latest.release.here")
}
```

## Features
- Out of the box zooming, using multi-touch and double-tap.
- Scrolling, with smooth scrolling fling.
- Works perfectly when used in a scrolling parent (such as ViewPager).
- Allows the application to be notified when the displayed Matrix has changed. Useful for when you need to update your UI based on the current zoom/scroll position.
- Allows the application to be notified when the user taps on the Photo.

## Usage
There is a [sample](https://github.com/Evolitist/PhotoView/tree/master/sample) provided which shows how to use the library in a more advanced way, but for completeness, here is all that is required to get PhotoView working:
```xml
<com.github.chrisbanes.photoview.PhotoView
    android:id="@+id/photoView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```
```kotlin
val photoView = findViewById<PhotoView>(R.id.photoView)
photoView.setImageResource(R.drawable.image)
```
That's it!

## Usage with Fresco
Due to the complex nature of Fresco, this library does not currently support Fresco. See [this project](https://github.com/ongakuer/PhotoDraweeView) as an alternative solution.

## Subsampling Support
This library aims to keep the zooming implementation simple. If you are looking for an implementation that supports subsampling, check out [this project](https://github.com/davemorrissey/subsampling-scale-image-view)

License
--------

    Copyright 2024 Evolitist
    Copyright 2018 Chris Banes

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
