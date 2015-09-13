package co.moonmonkeylabs.realmrecyclerview.example;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView;
import co.moonmonkeylabs.realmrecyclerview.example.models.QuoteModel;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

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
        setContentView(R.layout.activity_main);

        type = getIntent().getStringExtra("Type");
        setTitle(getResources().getString(R.string.activity_layout_name, type));

        resetRealm();
        realm = Realm.getInstance(this);

        realmRecyclerView = (RealmRecyclerView) findViewById(R.id.realm_recycler_view);

        if (type.equals("Grid")) {
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
            realmRecyclerView.setLayoutManager(gridLayoutManager);
        } else {
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            realmRecyclerView.setLayoutManager(linearLayoutManager);
        }
        
        RealmResults<QuoteModel> quoteModels =
                realm.where(QuoteModel.class).findAllSorted("id", false);
        quoteAdapter = new QuoteRecyclerViewAdapter(getBaseContext(), quoteModels, true);
        realmRecyclerView.setAdapter(quoteAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
        realm = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_quote) {
            asyncAddQuote();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class QuoteRecyclerViewAdapter extends RealmBasedRecyclerViewAdapter<QuoteModel,
            QuoteRecyclerViewAdapter.ViewHolder> {

        public QuoteRecyclerViewAdapter(
                Context context,
                RealmResults<QuoteModel> realmResults,
                boolean automaticUpdate) {
            super(context, realmResults, automaticUpdate);
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public FrameLayout container;
            public TextView quoteTextView;
            public ViewHolder(FrameLayout container) {
                super(container);
                this.container = container;
                this.quoteTextView = (TextView) container.findViewById(R.id.quote_text_view);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = inflater.inflate(R.layout.item_view, viewGroup, false);
            ViewHolder vh = new ViewHolder((FrameLayout) v);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int i) {
            final QuoteModel quoteModel = realmResults.get(i);
            viewHolder.quoteTextView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            asyncRemoveQuote(quoteModel.getId());
                        }
                    }
            );
            if (type != null && type.equals("Grid")) {
                viewHolder.container.setBackgroundColor(
                        colors.get((int) (quoteModel.getId() % colors.size())));
            }
            viewHolder.quoteTextView.setText(quoteModel.getQuote());
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

    private void resetRealm() {
        RealmConfiguration realmConfig = new RealmConfiguration
                .Builder(this)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.deleteRealm(realmConfig);
    }
}
