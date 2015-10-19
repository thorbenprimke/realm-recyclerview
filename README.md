## A RecyclerView that is powered by Realm

A powerful Recyclerview wrapper when working with Realm as your datastore. It supports the following out of the box:

* Custom adapter that automatically refreshes the list when the realm changes and animates the new items in.
* Empty state
* Optional pull-to-refresh (backed by SwipeRefreshLayout)
* Infinite scrolling (callback for more data fetching)
* Section headers

###How To Include It:

```
	 repositories {
        // ...
        maven { url "https://jitpack.io" }
    }
```

```
	dependencies {
	        compile 'com.github.thorbenprimke:realm-recyclerview:0.9'
	}
```

###Demo

A demo is in the example folder. Click the image below to play the video.

![Screenshot](extras/screenshot-demo-app.png)
[Play](extra/realm-recycler-view-demo.webm)