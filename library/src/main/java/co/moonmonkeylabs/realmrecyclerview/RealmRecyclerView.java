package co.moonmonkeylabs.realmrecyclerview;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import android.view.View;
import android.widget.FrameLayout;

/**
 * A recyclerView that has a few extra features.
 * - Automatic empty state
 */
public class RealmRecyclerView extends FrameLayout {

    private RecyclerView recyclerView;
    private FrameLayout emptyContentContainer;

    public RealmRecyclerView(Context context) {
        super(context);
        init();
    }

    public RealmRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RealmRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.realm_recycler_view, this);

        emptyContentContainer = (FrameLayout) findViewById(R.id.empty_content_container);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
    }

    public void setAdapter(final RecyclerView.Adapter adapter) {
        recyclerView.setAdapter(adapter);

        if (adapter != null) {
            adapter.registerAdapterDataObserver(
                    new RecyclerView.AdapterDataObserver() {
                        @Override
                        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                            super.onItemRangeMoved(fromPosition, toPosition, itemCount);
                            update();
                        }

                        @Override
                        public void onItemRangeRemoved(int positionStart, int itemCount) {
                            super.onItemRangeRemoved(positionStart, itemCount);
                            update();
                        }

                        @Override
                        public void onItemRangeInserted(int positionStart, int itemCount) {
                            super.onItemRangeInserted(positionStart, itemCount);
                            update();
                        }

                        @Override
                        public void onItemRangeChanged(int positionStart, int itemCount) {
                            super.onItemRangeChanged(positionStart, itemCount);
                            update();
                        }

                        @Override
                        public void onChanged() {
                            super.onChanged();
                            update();
                        }

                        private void update() {
                            emptyContentContainer.setVisibility(
                                    adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                        }
                    }
            );
            if (adapter.getItemCount() == 0) {
                emptyContentContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    public void setLayoutManager(RecyclerView.LayoutManager layoutManager) {
        recyclerView.setLayoutManager(layoutManager);
    }
}
