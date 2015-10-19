package co.moonmonkeylabs.realmrecyclerview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LoadMoreListItemView extends FrameLayout {

    TextView loadingText;

    ProgressBar spinner;

    public LoadMoreListItemView(Context context) {
        super(context);
        inflate(context, R.layout.load_more_item_view, this);

        loadingText = (TextView) findViewById(R.id.load_more_view_text);
        spinner = (ProgressBar) findViewById(R.id.load_more_spinner);

        setLayoutParams(new RealmRecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.load_more_item_height)));
    }

    public void hideLoadingText() {
        loadingText.setVisibility(View.GONE);
    }

    public void setLoadingText(int textResourceId) {
        loadingText.setText(textResourceId);
        loadingText.setVisibility(View.VISIBLE);
    }

    public void setLoadingText(String text) {
        loadingText.setText(text);
        loadingText.setVisibility(View.VISIBLE);
    }

    public void hideSpinner() {
        spinner.setVisibility(View.GONE);
    }

    public void showSpinner() {
        spinner.setVisibility(View.VISIBLE);
    }
}
