package co.moonmonkeylabs.realmrecyclerview.example;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class LayoutSelectorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout_selector);
        setTitle(R.string.activity_selector_name);

        addOnClickListenerToActivity(R.id.recycler_grid_button, GridExampleActivity.class, "Grid");

        addOnClickListenerToActivity(R.id.recycler_to_do_button, ToDoActivity.class, null);

        addOnClickListenerToActivity(R.id.recycler_linear_button, MainActivity.class, "Linear");

        addOnClickListenerToActivity(R.id.recycler_linear_bulk_button, MainActivity.class, "LinearBulk");

        addOnClickListenerToActivity(R.id.recycler_staggered_button, MainActivity.class, "Staggered");

        addOnClickListenerToActivity(R.id.recycler_linear_with_load_more_button, MainActivity.class, "LinearLoadMore");

        addOnClickListenerToActivity(R.id.recycler_section_header_button, MainActivity2.class, "Header (SLM) ");
    }

    private Button addOnClickListenerToActivity(@IdRes int viewId, final Class<?> activity, @Nullable final String typeExtra) {
        final Button button = (Button) findViewById(viewId);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LayoutSelectorActivity.this, activity);
                intent.putExtra("Type", typeExtra);
                startActivity(intent);
            }
        });
        return button;
    }
}
