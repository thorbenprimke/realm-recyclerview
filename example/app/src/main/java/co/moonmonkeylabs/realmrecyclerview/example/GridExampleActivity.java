package co.moonmonkeylabs.realmrecyclerview.example;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import co.moonmonkeylabs.realmnytimesdata.NYTimesDataLoader;
import co.moonmonkeylabs.realmnytimesdata.NYTimesModule;
import co.moonmonkeylabs.realmnytimesdata.model.NYTimesMultimedium;
import co.moonmonkeylabs.realmnytimesdata.model.NYTimesStory;
import co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmResults;
import io.realm.RealmViewHolder;
import io.realm.Sort;

public class GridExampleActivity extends AppCompatActivity {

    private static final String REPLACE_WITH_YOUR_API_KEY = "YOUR_API_KEY";
    private static final String NY_TIMES_API_KEY = REPLACE_WITH_YOUR_API_KEY;

    private RealmRecyclerView realmRecyclerView;
    private NYTimesStoryRecyclerViewAdapter nyTimesStoryAdapter;
    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_grid_layout);
        realmRecyclerView = (RealmRecyclerView) findViewById(R.id.realm_recycler_view);

        setTitle(getResources().getString(
                R.string.activity_layout_name,
                getIntent().getStringExtra("Type")));

        resetRealm();

        Realm.setDefaultConfiguration(getRealmConfig());
        realm = Realm.getDefaultInstance();
        RealmResults<NYTimesStory> nyTimesStories =
                realm.where(NYTimesStory.class).findAllSorted("sortTimeStamp", Sort.DESCENDING);
        nyTimesStoryAdapter = new NYTimesStoryRecyclerViewAdapter(this, nyTimesStories, true, true);
        realmRecyclerView.setAdapter(nyTimesStoryAdapter);

        if (NY_TIMES_API_KEY.equals(REPLACE_WITH_YOUR_API_KEY)) {
            Toast.makeText(this, "You need to add the NYTimes API key", Toast.LENGTH_LONG).show();
        } else {
            final NYTimesDataLoader nyTimesDataLoader = new NYTimesDataLoader();
            nyTimesDataLoader.loadAllData(realm, NY_TIMES_API_KEY);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
        realm = null;
    }

    public class NYTimesStoryRecyclerViewAdapter extends RealmBasedRecyclerViewAdapter<NYTimesStory,
            NYTimesStoryRecyclerViewAdapter.ViewHolder> {

        public NYTimesStoryRecyclerViewAdapter(
                Context context,
                RealmResults<NYTimesStory> realmResults,
                boolean automaticUpdate,
                boolean animateIdType) {
            super(context, realmResults, automaticUpdate, animateIdType);
        }

        public class ViewHolder extends RealmViewHolder {

            public TextView title;
            public TextView publishedDate;
            public ImageView image;
            public TextView storyAbstract;

            public ViewHolder(LinearLayout container) {
                super(container);
                this.title = (TextView) container.findViewById(R.id.title);
                this.publishedDate = (TextView) container.findViewById(R.id.date);
                this.image = (ImageView) container.findViewById(R.id.image);
                this.storyAbstract = (TextView) container.findViewById(R.id.story_abstract);
            }
        }

        @Override
        public ViewHolder onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
            View v = inflater.inflate(R.layout.grid_item_view, viewGroup, false);
            ViewHolder vh = new ViewHolder((LinearLayout) v);
            return vh;
        }

        @Override
        public void onBindRealmViewHolder(ViewHolder viewHolder, int position) {
            final NYTimesStory nyTimesStory = realmResults.get(position);
            viewHolder.title.setText(nyTimesStory.getTitle());
            viewHolder.publishedDate.setText(nyTimesStory.getPublishedDate());
            final RealmList<NYTimesMultimedium> multimedia = nyTimesStory.getMultimedia();
            if (multimedia != null && !multimedia.isEmpty()) {
                Glide.with(GridExampleActivity.this).load(
                        multimedia.get(0).getUrl()).into(viewHolder.image);
            } else {
                viewHolder.image.setImageResource(R.drawable.nytimes_logo);
            }
            viewHolder.storyAbstract.setText(nyTimesStory.getStoryAbstract());
        }
    }

    private RealmConfiguration getRealmConfig() {
        return new RealmConfiguration
                .Builder(this)
                .setModules(Realm.getDefaultModule(), new NYTimesModule())
                .build();
    }

    private void resetRealm() {
        Realm.deleteRealm(getRealmConfig());
    }
}
