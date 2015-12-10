package co.moonmonkeylabs.realmrecyclerview.example;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView;
import co.moonmonkeylabs.realmrecyclerview.example.models.QuoteModel;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.RealmViewHolder;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity {

    private static List<String> quotes = Arrays.asList(
            "Always borrow money from a pessimist. He won’t expect it back.",
            "Dogs have masters. Cats have staff.",
            "The best way to lie is to tell the truth . . . carefully edited truth.",
            "If at first you don’t succeed . . . so much for skydiving.",
            "A bargain is something you don’t need at a price you can’t resist.",
            "My mother never saw the irony in calling me a son-of-a-bitch.",
            "God gave us our relatives; thank God we can choose our friends.",
            "Women who seek to be equal with men lack ambition.",
            "If you do a job too well, you’ll get stuck with it.",
            "Insanity is hereditary. You get it from your children.");

    private static List<Integer> colors = Arrays.asList(
            Color.RED,
            Color.BLUE,
            Color.MAGENTA,
            Color.RED,
            Color.YELLOW);

    private RealmRecyclerView realmRecyclerView;
    private QuoteRecyclerViewAdapter quoteAdapter;
    private Realm realm;
    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        type = getIntent().getStringExtra("Type");
        if (type.equals("Grid")) {
            setContentView(R.layout.activity_main_grid_layout);
        } else {
            setContentView(R.layout.activity_main_linear_layout);
        }
        realmRecyclerView = (RealmRecyclerView) findViewById(R.id.realm_recycler_view);

        setTitle(getResources().getString(R.string.activity_layout_name, type));

        resetRealm();
        realm = Realm.getInstance(this);

        boolean isBulk = type.equals("LinearBulk");

        boolean isLoadMore = type.equals("LinearLoadMore");
        if (isLoadMore) {
            realm.beginTransaction();
            for (int i = 0; i < 60; i++) {
                QuoteModel quoteModel = realm.createObject(QuoteModel.class);
                quoteModel.setId(i + 1);
                quoteModel.setQuote(quotes.get((int) (quoteModel.getId() % quotes.size())));
            }
            realm.commitTransaction();
        }

        RealmResults<QuoteModel> quoteModels = realm
                .where(QuoteModel.class)
                .findAllSorted("id", (isLoadMore || isBulk) ? Sort.ASCENDING : Sort.DESCENDING);
        quoteAdapter = new QuoteRecyclerViewAdapter(
                getBaseContext(),
                quoteModels,
                true,
                true,
                isBulk ? "quote" : null);
        realmRecyclerView.setAdapter(quoteAdapter);

        realmRecyclerView.setOnRefreshListener(
                new RealmRecyclerView.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        asyncRefreshAllQuotes();
                    }
                }
        );

        if (isLoadMore) {
            realmRecyclerView.setOnLoadMoreListener(
                    new RealmRecyclerView.OnLoadMoreListener() {
                        @Override
                        public void onLoadMore(Object lastItem) {
                            if (lastItem instanceof QuoteModel) {
                                Toast.makeText(
                                        MainActivity.this,
                                        ((QuoteModel) lastItem).getId() + " ",
                                        Toast.LENGTH_SHORT).show();
                            }
                            asyncLoadMoreQuotes();
                        }
                    }
            );
            realmRecyclerView.enableShowLoadMore();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
        realm = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (type.equals("LinearBulk")) {
            getMenuInflater().inflate(R.menu.menu_bulk_main, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_main, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_quote) {
            asyncAddQuote();
            return true;
        } else if (id == R.id.action_bulk_add_initial) {
            asyncBulkAddInitial();
            return true;
        } else if (id == R.id.action_bulk_add_two) {
            asyncBulkAddTwo();
            return true;
        } else if (id == R.id.action_add_footer) {
            quoteAdapter.addFooter();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class QuoteRecyclerViewAdapter extends RealmBasedRecyclerViewAdapter<QuoteModel,
            QuoteRecyclerViewAdapter.ViewHolder> {

        public QuoteRecyclerViewAdapter(
                Context context,
                RealmResults<QuoteModel> realmResults,
                boolean automaticUpdate,
                boolean animateIdType,
                String animateExtraColumnName) {
            super(context, realmResults, automaticUpdate, animateIdType, animateExtraColumnName);
        }

        public class ViewHolder extends RealmViewHolder {

            public FrameLayout container;

            public TextView quoteTextView;

            public TextView footerTextView;

            public ViewHolder(FrameLayout container) {
                super(container);
                this.container = container;
                this.quoteTextView = (TextView) container.findViewById(R.id.quote_text_view);
                this.footerTextView = (TextView) container.findViewById(R.id.footer_text_view);
            }
        }

        @Override
        public ViewHolder onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
            View v = inflater.inflate(R.layout.item_view, viewGroup, false);
            ViewHolder vh = new ViewHolder((FrameLayout) v);
            return vh;
        }

        @Override
        public void onBindRealmViewHolder(ViewHolder viewHolder, int position) {
            final QuoteModel quoteModel = realmResults.get(position);
            viewHolder.quoteTextView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!quoteModel.isValid()) {
                                return;
                            }
                            asyncRemoveQuote(quoteModel.getId());
                        }
                    }
            );
            viewHolder.quoteTextView.setOnLongClickListener(
                    new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            asyncUpdateQuote(quoteModel.getId());
                            return true;
                        }
                    }
            );
            if (type != null && type.equals("Grid")) {
                viewHolder.container.setBackgroundColor(
                        colors.get((int) (quoteModel.getId() % colors.size())));
            }
            viewHolder.quoteTextView.setText(quoteModel.getQuote());
        }

        @Override
        public void onBindFooterViewHolder(ViewHolder holder, int position) {
            holder.footerTextView.setText("I'm a footer");
        }

        @Override
        public ViewHolder onCreateFooterViewHolder(ViewGroup viewGroup) {
            View v = inflater.inflate(R.layout.footer_view, viewGroup, false);
            ViewHolder vh = new ViewHolder((FrameLayout) v);
            return vh;
        }
    }

    private void asyncAddQuote() {
        AsyncTask<Void, Void, Void> remoteItem = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Realm instance = Realm.getInstance(MainActivity.this);
                instance.beginTransaction();
                QuoteModel quoteModel = instance.createObject(QuoteModel.class);
                quoteModel.setId(System.currentTimeMillis());
                quoteModel.setQuote(quotes.get((int) (quoteModel.getId() % quotes.size())));
                instance.commitTransaction();
                instance.close();
                return null;
            }
        };
        remoteItem.execute();
    }

    private void asyncBulkAddInitial() {
        AsyncTask<Void, Void, Void> remoteItem = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Realm instance = Realm.getInstance(MainActivity.this);
                QuoteModel cached = instance.where(QuoteModel.class).equalTo("id", 1).findFirst();
                if (cached != null) {
                    instance.close();
                    return null;
                }

                instance.beginTransaction();

                QuoteModel quoteModel = instance.createObject(QuoteModel.class);
                quoteModel.setId(1);
                quoteModel.setQuote(quotes.get((int) (quoteModel.getId() % quotes.size())));

                QuoteModel quoteModel2 = instance.createObject(QuoteModel.class);
                quoteModel2.setId(3);
                quoteModel2.setQuote(quotes.get((int) (quoteModel2.getId() % quotes.size())));

                QuoteModel quoteModel3 = instance.createObject(QuoteModel.class);
                quoteModel3.setId(5);
                quoteModel3.setQuote(quotes.get((int) (quoteModel3.getId() % quotes.size())));

                QuoteModel quoteModel4 = instance.createObject(QuoteModel.class);
                quoteModel4.setId(7);
                quoteModel4.setQuote(quotes.get((int) (quoteModel4.getId() % quotes.size())));

                instance.commitTransaction();
                instance.close();
                return null;
            }
        };
        remoteItem.execute();
    }

    private void asyncBulkAddTwo() {
        AsyncTask<Void, Void, Void> remoteItem = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Realm instance = Realm.getInstance(MainActivity.this);
                QuoteModel cached = instance.where(QuoteModel.class).equalTo("id", 2).findFirst();
                if (cached != null) {
                    instance.close();
                    return null;
                }
                instance.beginTransaction();

                QuoteModel quoteModel = instance.createObject(QuoteModel.class);
                quoteModel.setId(2);
                quoteModel.setQuote(quotes.get((int) (quoteModel.getId() % quotes.size())));

                QuoteModel quoteModel2 = instance.createObject(QuoteModel.class);
                quoteModel2.setId(4);
                quoteModel2.setQuote(quotes.get((int) (quoteModel2.getId() % quotes.size())));

                instance.commitTransaction();
                instance.close();
                return null;
            }
        };
        remoteItem.execute();
    }

    private void asyncRemoveQuote(final long id) {
        AsyncTask<Void, Void, Void> remoteItem = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Realm instance = Realm.getInstance(MainActivity.this);
                QuoteModel quoteModel =
                        instance.where(QuoteModel.class).equalTo("id", id).findFirst();
                if (quoteModel != null) {
                    instance.beginTransaction();
                    quoteModel.removeFromRealm();
                    instance.commitTransaction();
                }
                instance.close();
                return null;
            }
        };
        remoteItem.execute();
    }

    private void asyncUpdateQuote(final long id) {
        AsyncTask<Void, Void, Void> remoteItem = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Realm instance = Realm.getInstance(MainActivity.this);
                QuoteModel quoteModel =
                        instance.where(QuoteModel.class).equalTo("id", id).findFirst();
                if (quoteModel != null) {
                    instance.beginTransaction();
                    quoteModel.setQuote("Updated: " + quoteModel.getQuote());
                    instance.commitTransaction();
                }
                instance.close();
                return null;
            }
        };
        remoteItem.execute();
    }

    private void asyncRefreshAllQuotes() {
        AsyncTask<Void, Void, Void> remoteItem = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // Add some delay to the refresh/remove action.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                Realm instance = Realm.getInstance(MainActivity.this);
                final RealmResults<QuoteModel> all = instance.where(QuoteModel.class).findAll();
                if (all != null) {
                    instance.beginTransaction();
                    all.clear();
                    instance.commitTransaction();
                }
                instance.close();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                realmRecyclerView.setRefreshing(false);
            }
        };
        remoteItem.execute();
    }

    private void asyncLoadMoreQuotes() {
        AsyncTask<Void, Void, Void> remoteItem = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // Add some delay to the refresh/remove action.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                Realm instance = Realm.getInstance(MainActivity.this);
                instance.beginTransaction();
                for (int i = 0; i < 60; i++) {
                    QuoteModel quoteModel = instance.createObject(QuoteModel.class);
                    quoteModel.setId(i + 100); // That is to offset for primary key
                    quoteModel.setQuote(quotes.get((int) (quoteModel.getId() % quotes.size())));
                }
                instance.commitTransaction();
                instance.close();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                realmRecyclerView.disableShowLoadMore();
            }
        };
        remoteItem.execute();
    }

    private void resetRealm() {
        RealmConfiguration realmConfig = new RealmConfiguration
                .Builder(this)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.deleteRealm(realmConfig);
    }
}
