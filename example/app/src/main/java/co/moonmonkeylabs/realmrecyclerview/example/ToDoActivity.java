package co.moonmonkeylabs.realmrecyclerview.example;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView;
import co.moonmonkeylabs.realmrecyclerview.example.models.TodoItem;
import io.realm.Realm;
import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.RealmViewHolder;
import io.realm.Sort;

/**
 * A TO-DO app example showcasing the {@link RealmRecyclerView} with swipe to delete.
 */
public class ToDoActivity extends AppCompatActivity {

    private static final int[] COLORS = new int[] {
            Color.argb(255, 28, 160, 170),
            Color.argb(255, 99, 161, 247),
            Color.argb(255, 13, 79, 139),
            Color.argb(255, 89, 113, 173),
            Color.argb(255, 200, 213, 219),
            Color.argb(255, 99, 214, 74),
            Color.argb(255, 205, 92, 92),
            Color.argb(255, 105, 5, 98)
    };

    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.to_do_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buildAndShowInputDialog();
            }
        });

        resetRealm();
        realm = Realm.getInstance(this);
        RealmResults<TodoItem> toDoItems = realm
                .where(TodoItem.class)
                .findAllSorted("id", Sort.ASCENDING);
        ToDoRealmAdapter toDoRealmAdapter =
                new ToDoRealmAdapter(this, toDoItems, true, true);
        RealmRecyclerView realmRecyclerView =
                (RealmRecyclerView) findViewById(R.id.realm_recycler_view);
        realmRecyclerView.setAdapter(toDoRealmAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realm != null) {
            realm.close();
            realm = null;
        }
    }

    private void buildAndShowInputDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(ToDoActivity.this);
        builder.setTitle("Create A Task");

        LayoutInflater li = LayoutInflater.from(this);
        View dialogView = li.inflate(R.layout.to_do_dialog_view, null);
        final EditText input = (EditText) dialogView.findViewById(R.id.input);

        builder.setView(dialogView);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                addToDoItem(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        final AlertDialog dialog = builder.show();
        input.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE ||
                                (event.getAction() == KeyEvent.ACTION_DOWN &&
                                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                            dialog.dismiss();
                            addToDoItem(input.getText().toString());
                            return true;
                        }
                        return false;
                    }
                });
    }

    private void addToDoItem(String toDoItemText) {
        if (toDoItemText == null || toDoItemText.length() == 0) {
            Toast
                    .makeText(this, "Empty ToDos don't get stuff done!", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        realm.beginTransaction();
        TodoItem todoItem = realm.createObject(TodoItem.class);
        todoItem.setId(System.currentTimeMillis());
        todoItem.setDescription(toDoItemText);
        realm.commitTransaction();
    }

    public class ToDoRealmAdapter
            extends RealmBasedRecyclerViewAdapter<TodoItem, ToDoRealmAdapter.ViewHolder> {

        public class ViewHolder extends RealmViewHolder {

            public TextView todoTextView;
            public ViewHolder(FrameLayout container) {
                super(container);
                this.todoTextView = (TextView) container.findViewById(R.id.todo_text_view);
            }
        }

        public ToDoRealmAdapter(
                Context context,
                RealmResults<TodoItem> realmResults,
                boolean automaticUpdate,
                boolean animateResults) {
            super(context, realmResults, automaticUpdate, animateResults);
        }

        @Override
        public ViewHolder onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
            View v = inflater.inflate(R.layout.to_do_item_view, viewGroup, false);
            ViewHolder vh = new ViewHolder((FrameLayout) v);
            return vh;
        }

        @Override
        public void onBindRealmViewHolder(ViewHolder viewHolder, int position) {
            final TodoItem toDoItem = realmResults.get(position);
            viewHolder.todoTextView.setText(toDoItem.getDescription());
            viewHolder.itemView.setBackgroundColor(
                    COLORS[(int) (toDoItem.getId() % COLORS.length)]
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
}
