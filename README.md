# A RecyclerView that is powered by Realm

A powerful ```Recyclerview``` wrapper for working with ```Realm``` as your datastore. It supports the following features out of the box:

* Custom adapter that automatically refreshes the list when the realm changes and animates the new items in.
* Empty state
* Pull-to-refresh (backed by SwipeRefreshLayout)
* Infinite scrolling (callback for more data fetching)
* Section headers (backed by SuperSLiM)

##How To Include It:

```
	repositories {
        // ...
        maven { url "https://jitpack.io" }
    }
```

```
	dependencies {
	        compile 'com.github.thorbenprimke:realm-recyclerview:0.9.12'
	}
```

##Demo

![Screenshot](https://raw.githubusercontent.com/thorbenprimke/realm-recyclerview/master/extra/screenshot-demo-app.gif)

## How To Get Started:

The ```RealmRecyclerView``` has a few attributes that can be set in XML in order to customize it's look and feel and most importanlty which layoutType is used. In addition, it relies on an extended ```RecyclerView.Adapter``` called ```RealmBasedRecyclerViewAdapter``` to provide support for animation and headers.

##RealmRecyclerView

The snippet below shows how to include the ```RealmRecyclerView``` in your  layout file.

```
    <co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView
        android:id="@+id/realm_recycler_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:rrvIsRefreshable="true"
        app:rrvEmptyLayoutId="@layout/empty_view"
        app:rrvLayoutType="LinearLayout"
        />
```

Important to note here is that the ```app:rrvLayoutType``` attribute has to be set. It determines which ```LayoutManager``` will be used. The options are:

* ```LinearLayout```
* ```Grid```
* ```LinearLayoutWithHeaders```

All these will yield vertical linear or grid layouts.

###Other Attributes:

```rrvIsRefreshable```: Adds the pull-to-refresh feature to the ```recyclerView```. In order to receive the refresh events, a listner has to be set via ```setOnRefreshListener``` and ```setRefreshing``` is used to control either turn the refersh animation on/off.

```rrvEmptyLayoutId```: A custom empty state view can be provided via this attribute. Whenever the list has no item, the empty state is shown. 

```rrvGridLayoutSpanCount```: This attribute has to be set with an integer greater than zero when the ```rrvLayoutType``` is set to ```Grid``` unless ```rrvGridLayoutItemWidth``` is set.

 ```rrvGridLayoutItemWidth```: This attribute has to be set with a size value that represents the width of a grid column when the ```rrvLayoutType``` is set to ```Grid``` unless ```rrvGridLayoutSpanCount``` is set.

```rrvSwipeToDelete```: This attribute is only supported with ```rrvLayoutType``` of ```LinearLayout```. If set to true, swiping a row to delete is enabled. The row is deleted from the ```Realm``` directly.

##RealmBasedRecyclerViewAdapter: 

The heart of the ```RealmRecyclerView```'s functionality comes from this custom ```RecyclerView.Adapter```. It includes support for insertion/deletion animation whenever the ```Realm``` changes. It also inculde the logic to generate the headers for the list's contents if it's of type ```LinearLayoutWithHeaders```. 

* ```automaticUpdate```: If automaticUpdate is set, the ```RealmResults``` are automatially updated and the list is refershed with new results.

* ```animateResults```: If animateResults is set together with automaticUpdate, the automatic updates are animated. This is limited to a single deletion or insertion. If it's more than one item, it will simply refresh the list. The animation leverages the resuls primary key column in order as a unique identifier for each row. Therefore your ```Realm```'s schema needs to include a primary key column of type ```Integer``` or ```String```.

* ```addSectionHeaders```: When the ```rrvLayoutType``` is ```LinearLayoutWithHeaders```, addSectionHeaders needs be set in order for the adapter to generate the headers. The ```headerColumnName``` needs to be set as well in order to look up the header column programmatically your ```Realm```'s schema. *Note: There is currently no support for customizing the header and it is always inline|sticky.*

##Feedback/More Features:
I would love to hear your feedback. Do you find the ```RealmRecyclerView``` useful? What functionality are you missing? Open a ```Github``` issue and let me know. Thanks!


## License
```
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Included dependencies are:
Realm (https://github.com/realm/realm-java)
SuperSLiM/Tonic Artos (https://github.com/TonicArtos/SuperSLiM)
```
