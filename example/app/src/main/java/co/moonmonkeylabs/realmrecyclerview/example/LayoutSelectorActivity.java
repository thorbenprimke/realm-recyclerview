package co.moonmonkeylabs.realmrecyclerview.example;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class LayoutSelectorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout_selector);
        setTitle(R.string.activity_selector_name);

        final Button linearButton = (Button) findViewById(R.id.recycler_linear_button);
        linearButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(LayoutSelectorActivity.this, MainActivity.class);
                        intent.putExtra("Type", "Linear");
                        startActivity(intent);
                    }
                }
        );

        final Button linearBulkButton = (Button) findViewById(R.id.recycler_linear_bulk_button);
        linearBulkButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(LayoutSelectorActivity.this, MainActivity.class);
                        intent.putExtra("Type", "LinearBulk");
                        startActivity(intent);
                    }
                }
        );

        final Button linearWithLoadMoreButton =
                (Button) findViewById(R.id.recycler_linear_with_load_more_button);
        linearWithLoadMoreButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(LayoutSelectorActivity.this, MainActivity.class);
                        intent.putExtra("Type", "LinearLoadMore");
                        startActivity(intent);
                    }
                }
        );


        final Button gridButton = (Button) findViewById(R.id.recycler_grid_button);
        gridButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent =
                                new Intent(LayoutSelectorActivity.this, GridExampleActivity.class);
                        intent.putExtra("Type", "Grid");
                        startActivity(intent);
                    }
                }
        );

        final Button sectionButton = (Button) findViewById(R.id.recycler_section_header_button);
        sectionButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(LayoutSelectorActivity.this, MainActivity2.class);
                        intent.putExtra("Type", "Header (SLM) ");
                        startActivity(intent);
                    }
                }
        );

        final Button toDoButton = (Button) findViewById(R.id.recycler_to_do_button);
        toDoButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(LayoutSelectorActivity.this, ToDoActivity.class);
                        startActivity(intent);
                    }
                }
        );
    }
}
