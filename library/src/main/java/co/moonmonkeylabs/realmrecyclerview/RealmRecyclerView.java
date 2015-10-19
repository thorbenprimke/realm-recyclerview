package co.moonmonkeylabs.realmrecyclerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.tonicartos.superslim.LayoutManager;

import io.realm.RealmBasedRecyclerViewAdapter;

/**
 * A recyclerView that has a few extra features.
 * - Automatic empty state
 * - Pull-to-refresh
 * - LoadMore
 */
public class RealmRecyclerView extends FrameLayout {

    public interface OnRefreshListener {
        void onRefresh();
    }

    public interface OnLoadMoreListener {
        void onLoadMore(Object lastItem);
    }

    private enum Type {
        LinearLayout,
        Grid,
        LinearLayoutWithHeaders
    }

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ViewStub emptyContentContainer;
    private RealmBasedRecyclerViewAdapter adapter;
    private boolean hasLoadMoreFired;
    private boolean showShowLoadMore;

    // Attributes
    private boolean isRefreshable;
    private int emptyViewId;
    private Type type;
    private int gridSpanCount;

    // State
    private boolean isRefreshing;

    // Listener
    private OnRefreshListener onRefreshListener;
    private OnLoadMoreListener onLoadMoreListener;

    public RealmRecyclerView(Context context) {
        super(context);
        init(context, null);
    }

    public RealmRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RealmRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.realm_recycler_view, this);
        initAttrs(context, attrs);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.rrv_swipe_refresh_layout);
        recyclerView = (RecyclerView) findViewById(R.id.rrv_recycler_view);
        emptyContentContainer = (ViewStub) findViewById(R.id.rrv_empty_content_container);

        swipeRefreshLayout.setEnabled(isRefreshable);
        if (isRefreshable) {
            swipeRefreshLayout.setOnRefreshListener(recyclerViewRefreshListener);
        }

        if (emptyViewId != 0) {
            emptyContentContainer.setLayoutResource(emptyViewId);
            emptyContentContainer.inflate();
        }

        if (type == null) {
            throw new IllegalStateException("A type has to be specified via XML attribute");
        }
        switch (type) {
            case LinearLayout:
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                break;

            case Grid:
                if (gridSpanCount == -1) {
                    throw new IllegalStateException("For GridLayout, a span count has to be set");
                }
                recyclerView.setLayoutManager(new GridLayoutManager(getContext(), gridSpanCount));
                break;

            case LinearLayoutWithHeaders:
                recyclerView.setLayoutManager(new LayoutManager(getContext()));
                break;
        }
        recyclerView.setHasFixedSize(true);

        recyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                    }
                }
        );

        recyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                        super.onScrollStateChanged(recyclerView, newState);
                    }

                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        maybeFireLoadMore();
                    }
                }
        );
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    public void enableShowLoadMore() {
        showShowLoadMore = true;
        ((RealmBasedRecyclerViewAdapter) recyclerView.getAdapter()).addLoadMore();
    }

    public void disableShowLoadMore() {
        showShowLoadMore = false;
        ((RealmBasedRecyclerViewAdapter) recyclerView.getAdapter()).removeLoadMore();
    }

    private void maybeFireLoadMore() {
        if (hasLoadMoreFired) {
            return;
        }
        if (!showShowLoadMore) {
            return;
        }

        final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();
        int firstVisibleItemPosition = findFirstVisibleItemPosition();

        if (totalItemCount == 0) {
            return;
        }

        if (firstVisibleItemPosition + visibleItemCount + 3 > totalItemCount) {
            if (onLoadMoreListener != null) {
                hasLoadMoreFired = true;
                onLoadMoreListener.onLoadMore(adapter.getLastItem());
            }
        }
    }

    public int findFirstVisibleItemPosition() {
        switch (type) {
            case LinearLayout:
                return ((LinearLayoutManager) recyclerView.getLayoutManager())
                        .findFirstVisibleItemPosition();
            case Grid:
                return ((GridLayoutManager) recyclerView.getLayoutManager())
                        .findFirstVisibleItemPosition();
            case LinearLayoutWithHeaders:
                return ((LayoutManager) recyclerView.getLayoutManager())
                        .findFirstVisibleItemPosition();
            default:
                throw new IllegalStateException("Type of layoutManager unknown." +
                        "In this case this method needs to be overridden");
        }
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray =
                context.obtainStyledAttributes(attrs, R.styleable.RealmRecyclerView);

        isRefreshable =
                typedArray.getBoolean(R.styleable.RealmRecyclerView_rrvIsRefreshable, false);
        emptyViewId =
                typedArray.getResourceId(R.styleable.RealmRecyclerView_rrvEmptyLayoutId, 0);
        int typeValue = typedArray.getInt(R.styleable.RealmRecyclerView_rrvLayoutType, -1);
        if (typeValue != -1) {
            type = Type.values()[typeValue];
        }
        gridSpanCount = typedArray.getInt(R.styleable.RealmRecyclerView_rrvGridLayoutSpanCount, -1);
        typedArray.recycle();
    }

    public void setAdapter(final RealmBasedRecyclerViewAdapter adapter) {
        this.adapter = adapter;
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
                            updateEmptyContentContainerVisibility(adapter);
                        }
                    }
            );
            updateEmptyContentContainerVisibility(adapter);
        }
    }

    private void updateEmptyContentContainerVisibility(RecyclerView.Adapter adapter) {
        if (emptyViewId == 0) {
            return;
        }
        emptyContentContainer.setVisibility(
                adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    //
    // Pull-to-refresh
    //

    /**
     * Only if custom is set for the manager, this method should be used to set the manager.
     */
    public void setLayoutManager(RecyclerView.LayoutManager layoutManger) {
        recyclerView.setLayoutManager(layoutManger);
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.onRefreshListener = onRefreshListener;
    }

    public void setRefreshing(boolean refreshing) {
        if (!isRefreshable) {
            return;
        }
        isRefreshing = refreshing;
        swipeRefreshLayout.setRefreshing(refreshing);
    }

    private SwipeRefreshLayout.OnRefreshListener recyclerViewRefreshListener =
            new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    if (!isRefreshing && onRefreshListener != null) {
                        onRefreshListener.onRefresh();
                    }
                    isRefreshing = true;
                }
            };
}
