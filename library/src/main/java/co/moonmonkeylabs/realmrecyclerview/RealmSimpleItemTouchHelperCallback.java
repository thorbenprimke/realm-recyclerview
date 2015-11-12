package co.moonmonkeylabs.realmrecyclerview;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import io.realm.RealmBasedRecyclerViewAdapter;

/**
 * Implementation of {@link ItemTouchHelper.Callback} for supporting swipe gesture.
 * Adapted from: https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf
 */
public class RealmSimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {

    private RealmBasedRecyclerViewAdapter adapter;

    public RealmSimpleItemTouchHelperCallback() {
    }

    public void setAdapter(RealmBasedRecyclerViewAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(0, swipeFlags);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        adapter.onItemSwipedDismiss(viewHolder.getAdapterPosition());
    }
}
