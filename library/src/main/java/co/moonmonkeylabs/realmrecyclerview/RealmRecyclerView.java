package co.moonmonkeylabs.realmrecyclerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;

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
    private RealmSimpleItemTouchHelperCallback realmSimpleItemTouchHelperCallback;
    private boolean hasLoadMoreFired;
    private boolean showShowLoadMore;

    // Attributes
    private boolean isRefreshable;
    private int emptyViewId;
    private Type type;
    private int gridSpanCount;
    private int gridWidthPx;
    private boolean swipeToDelete;

    private GridLayoutManager gridManager;
    private int lastMeasuredWidth = -1;

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

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (gridWidthPx != -1 && gridManager != null && lastMeasuredWidth != getMeasuredWidth()) {
            int spanCount = Math.max(1, getMeasuredWidth() / gridWidthPx);
            gridManager.setSpanCount(spanCount);
            lastMeasuredWidth = getMeasuredWidth();
        }
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
                throwIfSwipeToDeleteEnabled();
                if (gridSpanCount == -1 && gridWidthPx == -1) {
                    throw new IllegalStateException(
                            "For GridLayout, a span count or item width has to be set");
                } else if(gridSpanCount != -1 && gridWidthPx != -1) {
                    // This is awkward. Both values are set. Instead of picking one, throw an error.
                    throw new IllegalStateException(
                            "For GridLayout, a span count and item width can not both be set");
                }
                // Uses either the provided gridSpanCount or 1 as a placeholder what will be
                // calculated based on gridWidthPx in onMeasure.
                int spanCount = gridSpanCount == -1 ? 1 : gridSpanCount;
                gridManager = new GridLayoutManager(getContext(), spanCount);
                recyclerView.setLayoutManager(gridManager);
                break;

            case LinearLayoutWithHeaders:
                throwIfSwipeToDeleteEnabled();
                recyclerView.setLayoutManager(new LayoutManager(getContext()));
                break;

            default:
                throw new IllegalStateException("The type attribute has to be set.");
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

        if (swipeToDelete) {
            realmSimpleItemTouchHelperCallback = new RealmSimpleItemTouchHelperCallback();
            new ItemTouchHelper(realmSimpleItemTouchHelperCallback)
                    .attachToRecyclerView(recyclerView);
        }
    }

    private void throwIfSwipeToDeleteEnabled() {
        if (!swipeToDelete) {
            return;
        }
        throw new IllegalStateException(
                "SwipeToDelete not supported with this layout type: " + type.name());
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
        gridWidthPx = typedArray
                .getDimensionPixelSize(R.styleable.RealmRecyclerView_rrvGridLayoutItemWidth, -1);
        swipeToDelete =
                typedArray.getBoolean(R.styleable.RealmRecyclerView_rrvSwipeToDelete, false);
        typedArray.recycle();
    }

    public void setAdapter(final RealmBasedRecyclerViewAdapter adapter) {
        this.adapter = adapter;
        recyclerView.setAdapter(adapter);
        if (swipeToDelete) {
            realmSimpleItemTouchHelperCallback.setAdapter(adapter);
        }

        if (adapter != null) {
            adapter.registerAdapterDataObserver(
                    new RecyclerView.AdapterDataObserver() {
                        @Override
                        public void onItemRangeMoved(
                                int fromPosition,
                                int toPosition,
                                int itemCount) {
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
    // Expose public RecyclerView methods to the RealmRecyclerView
    //
    
    
    public void setItemViewCacheSize(int size) {
        recyclerView.setItemViewCacheSize(size);
    }

    public void smoothScrollToPosition(int position) {
        recyclerView.smoothScrollToPosition(position);
    }

    //
    // Pull-to-refresh
    //

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
