package co.moonmonkeylabs.realmrecyclerview.example;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView;
import co.moonmonkeylabs.realmrecyclerview.example.models.CountryModel;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.RealmViewHolder;
import io.realm.Sort;

public class MainActivity2 extends AppCompatActivity {

    private RealmRecyclerView realmRecyclerView;
    private CountryRecyclerViewAdapter countryAdapter;
    private Realm realm;
    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_linear_layout_headers);

        type = getIntent().getStringExtra("Type");
        setTitle(getResources().getString(R.string.activity_layout_name, type));

        resetRealm();
        realm = Realm.getInstance(this);

        realmRecyclerView = (RealmRecyclerView) findViewById(R.id.realm_recycler_view);

        // Init Realm with n country names
        final String[] countryNames = getResources().getStringArray(R.array.country_names);
        realm.beginTransaction();
        for (int i = 0; i < countryNames.length; i++) {
            CountryModel countryModel = new CountryModel(i, countryNames[i]);
            realm.copyToRealm(countryModel);
        }
        realm.commitTransaction();

        RealmResults<CountryModel> countryModels =
                realm.where(CountryModel.class).findAllSorted("name", Sort.ASCENDING);
        countryAdapter = new CountryRecyclerViewAdapter(getBaseContext(), countryModels);
        realmRecyclerView.setAdapter(countryAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
        realm = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_header_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.add_b_tiem) {
            asyncAddCountry("Belgium");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class CountryRecyclerViewAdapter extends RealmBasedRecyclerViewAdapter<
            CountryModel, CountryRecyclerViewAdapter.ViewHolder> {

        public CountryRecyclerViewAdapter(
                Context context,
                RealmResults<CountryModel> realmResults) {
            super(context, realmResults, true, true, true, "name");
        }

        public class ViewHolder extends RealmViewHolder {

            public FrameLayout container;

            public TextView countryTextView;

            public ViewHolder(FrameLayout container) {
                super(container);
                this.container = container;
                this.countryTextView = (TextView) container.findViewById(R.id.quote_text_view);
            }
        }

        @Override
        public ViewHolder onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
            View v = inflater.inflate(R.layout.item_view, viewGroup, false);
            return new ViewHolder((FrameLayout) v);
        }

        @Override
        public void onBindRealmViewHolder(ViewHolder viewHolder, int position) {
            final CountryModel quoteModel = realmResults.get(position);
            viewHolder.countryTextView.setText(quoteModel.getName());
            viewHolder.countryTextView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            asyncRemoveCountry(quoteModel.getId());
                        }
                    }
            );
        }
    }

    private void resetRealm() {
        RealmConfiguration realmConfig = new RealmConfiguration
                .Builder(this)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.deleteRealm(realmConfig);
    }

    private void asyncRemoveCountry(final long id) {
        AsyncTask<Void, Void, Void> remoteItem = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Realm instance = Realm.getInstance(MainActivity2.this);
                CountryModel countryModel =
                        instance.where(CountryModel.class).equalTo("id", id).findFirst();
                if (countryModel != null) {
                    instance.beginTransaction();
                    countryModel.removeFromRealm();
                    instance.commitTransaction();
                }
                instance.close();
                return null;
            }
        };
        remoteItem.execute();
    }

    private void asyncAddCountry(final String name) {
        AsyncTask<Void, Void, Void> remoteItem = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Realm instance = Realm.getInstance(MainActivity2.this);
                instance.beginTransaction();
                CountryModel countryModel =
                        instance.where(CountryModel.class).equalTo("name", name).findFirst();
                if (countryModel == null) {
                    CountryModel newCountryModel = new CountryModel(1000, name);
                    instance.copyToRealm(newCountryModel);
                    instance.commitTransaction();
                }
                instance.close();
                return null;
            }
        };
        remoteItem.execute();
    }
}
