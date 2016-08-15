/*
 * Originally based on io.realm.RealmBaseAdapter
 * =============================================
 * Copyright 2014 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.realm;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tonicartos.superslim.GridSLM;
import com.tonicartos.superslim.LinearSLM;

import java.util.ArrayList;
import java.util.List;

import co.moonmonkeylabs.realmrecyclerview.LoadMoreListItemView;
import co.moonmonkeylabs.realmrecyclerview.R;
import co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView;
import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import io.realm.internal.RealmObjectProxy;
import io.realm.internal.Row;
import io.realm.internal.TableOrView;

/**
 * The base {@link RecyclerView.Adapter} that includes custom functionality to be used with the
 * {@link RealmRecyclerView}.
 *
 * Ensure {@link #close()} is called whenever {@link Realm#close()} is called to ensure that the
 * {@link #realmResults} are invalidated and the change listener removed.
 */
public abstract class RealmBasedRecyclerViewAdapter
        <T extends RealmModel, VH extends RealmViewHolder>
        extends RecyclerView.Adapter<RealmViewHolder> {

    public class RowWrapper {

        public final boolean isRealm;
        public final int realmIndex;
        public final int sectionHeaderIndex;
        public final String header;

        public RowWrapper(int realmIndex, int sectionHeaderIndex) {
            this(true, realmIndex, sectionHeaderIndex, null);
        }

        public RowWrapper(int sectionHeaderIndex, String header) {
            this(false, -1, sectionHeaderIndex, header);
        }

        public RowWrapper(boolean isRealm, int realmIndex, int sectionHeaderIndex, String header) {
            this.isRealm = isRealm;
            this.realmIndex = realmIndex;
            this.sectionHeaderIndex = sectionHeaderIndex;
            this.header = header;
        }
    }

    public static final long NO_SECTION_HEADER_COLUMN = -1L;
    private static final List<Long> EMPTY_LIST = new ArrayList<>(0);

    private Object loadMoreItem;
    private Object footerItem;

    protected final int HEADER_VIEW_TYPE = 100;
    private final int LOAD_MORE_VIEW_TYPE = 101;
    private final int FOOTER_VIEW_TYPE = 102;

    private Context context;
    protected LayoutInflater inflater;
    protected OrderedRealmCollection<T> realmResults;
    protected List ids;

    private List<RowWrapper> rowWrappers;

    private RealmChangeListener<OrderedRealmCollection<T>> listener;
    private boolean automaticUpdate;
    private boolean animateResults;
    private boolean addSectionHeaders;
    private String headerColumnName;

    private long animatePrimaryColumnIndex;
    private RealmFieldType animatePrimaryIdType;
    private long animateExtraColumnIndex;
    private RealmFieldType animateExtraIdType;

    public RealmBasedRecyclerViewAdapter(
            Context context,
            OrderedRealmCollection<T> realmResults,
            boolean automaticUpdate,
            boolean animateResults,
            String animateExtraColumnName) {
        this(
                context,
                realmResults,
                automaticUpdate,
                animateResults,
                false,
                null,
                animateExtraColumnName);
    }

    public RealmBasedRecyclerViewAdapter(
            Context context,
            OrderedRealmCollection<T> realmResults,
            boolean automaticUpdate,
            boolean animateResults) {
        this(context, realmResults, automaticUpdate, animateResults, false, null);
    }

    public RealmBasedRecyclerViewAdapter(
            Context context,
            OrderedRealmCollection<T> realmResults,
            boolean automaticUpdate,
            boolean animateResults,
            boolean addSectionHeaders) {
        this(
                context,
                realmResults,
                automaticUpdate,
                animateResults,
                addSectionHeaders,
                null,
                null);
    }

    public RealmBasedRecyclerViewAdapter(
            Context context,
            OrderedRealmCollection<T> realmResults,
            boolean automaticUpdate,
            boolean animateResults,
            boolean addSectionHeaders,
            String headerColumnName) {
        this(
                context,
                realmResults,
                automaticUpdate,
                animateResults,
                addSectionHeaders,
                headerColumnName,
                null);
    }

    public RealmBasedRecyclerViewAdapter(
            Context context,
            OrderedRealmCollection<T> realmResults,
            boolean automaticUpdate,
            boolean animateResults,
            boolean addSectionHeaders,
            String headerColumnName,
            String animateExtraColumnName) {

        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        this.context = context;
        this.animateResults = animateResults;
        this.addSectionHeaders = addSectionHeaders;
        this.headerColumnName = headerColumnName;
        this.inflater = LayoutInflater.from(context);
        this.automaticUpdate = automaticUpdate;
        this.listener = (!automaticUpdate) ? null : getRealmChangeListener();
        this.rowWrappers = new ArrayList<>();

        // If automatic updates aren't enabled, then animateResults should be false as well.
        this.animateResults = (automaticUpdate && animateResults);
        if (animateResults) {
            setupAnimateResults(realmResults, animateExtraColumnName);
        }

        updateRealmResults(realmResults);
    }

    private void setupAnimateResults(OrderedRealmCollection<T> realmResults, String animateExtraColumnName) {
        TableOrView table = getTableOrView(realmResults);

        animatePrimaryColumnIndex = table.getTable().getPrimaryKey();
        if (animatePrimaryColumnIndex == TableOrView.NO_MATCH) {
            throw new IllegalStateException("Animating the results requires a primaryKey.");
        }

        animatePrimaryIdType = table.getColumnType(animatePrimaryColumnIndex);
        if (animatePrimaryIdType != RealmFieldType.INTEGER && animatePrimaryIdType != RealmFieldType.STRING) {
            throw new IllegalStateException("Animating requires a primary key of type Integer/Long or String");
        }

        if (animateExtraColumnName != null) {
            animateExtraColumnIndex = table.getTable().getColumnIndex(animateExtraColumnName);
            if (animateExtraColumnIndex == TableOrView.NO_MATCH) {
                throw new IllegalStateException("Animating the results requires a valid animateColumnName.");
            }

            animateExtraIdType = table.getColumnType(animateExtraColumnIndex);
            if (animateExtraIdType != RealmFieldType.INTEGER &&
                    animateExtraIdType != RealmFieldType.STRING &&
                    animateExtraIdType != RealmFieldType.DATE) {
                throw new IllegalStateException("Animating requires a animateColumnName of type Int/Long or String");
            }
        } else {
            animateExtraColumnIndex = -1;
        }
    }

    public abstract VH onCreateRealmViewHolder(ViewGroup viewGroup, int viewType);

    public abstract void onBindRealmViewHolder(VH holder, int position);

    public VH onCreateFooterViewHolder(ViewGroup viewGroup) {
        throw new IllegalStateException("Implementation missing");
    }

    public void onBindFooterViewHolder(VH holder, int position) {
        throw new IllegalStateException("Implementation missing");
    }

    public RealmViewHolder onCreateHeaderViewHolder(ViewGroup viewGroup) {
        View view = inflater.inflate(R.layout.header_item, viewGroup, false);
        return new RealmViewHolder((TextView) view);
    }

    public void onBindHeaderViewHolder(RealmViewHolder holder, int position) {
        String header = rowWrappers.get(position).header;
        final GridSLM.LayoutParams layoutParams =
            GridSLM.LayoutParams.from(holder.itemView.getLayoutParams());

        holder.headerTextView.setText(header);
        if (layoutParams.isHeaderInline()) {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        } else {
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
    }

    public Context getContext() {
        return context;
    }

    /**
     * DON'T OVERRIDE THIS METHOD. Implement onCreateRealmViewHolder instead.
     */
    @Override
    public final RealmViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == HEADER_VIEW_TYPE) {
            return onCreateHeaderViewHolder(viewGroup);
        } else if (viewType == LOAD_MORE_VIEW_TYPE) {
            return new RealmViewHolder(new LoadMoreListItemView(viewGroup.getContext()));
        } else if (viewType == FOOTER_VIEW_TYPE) {
            return onCreateFooterViewHolder(viewGroup);
        }

        return onCreateRealmViewHolder(viewGroup, viewType);
    }

    /**
     * DON'T OVERRIDE THIS METHOD. Implement onBindRealmViewHolder instead.
     */
    @Override
    @SuppressWarnings("unchecked")
    public final void onBindViewHolder(RealmViewHolder holder, int position) {
        if (getItemViewType(position) == LOAD_MORE_VIEW_TYPE) {
            holder.loadMoreView.showSpinner();
        } else if (getItemViewType(position) == FOOTER_VIEW_TYPE) {
            onBindFooterViewHolder((VH) holder, position);
        } else {
            if (addSectionHeaders) {
                String header = rowWrappers.get(position).header;
                ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
                GridSLM.LayoutParams layoutParams = GridSLM.LayoutParams.from(params);

                // Setup the header
                if (header != null) {
                    layoutParams.isHeader = true;
                    onBindHeaderViewHolder(holder,position);
                } else {
                    onBindRealmViewHolder((VH) holder, rowWrappers.get(position).realmIndex);
                }

                layoutParams.setSlm(LinearSLM.ID);

                if (header != null) {
                    layoutParams.setFirstPosition(position);
                } else {
                    layoutParams.setFirstPosition(rowWrappers.get(position).sectionHeaderIndex);
                }

                holder.itemView.setLayoutParams(layoutParams);
            } else {
                onBindRealmViewHolder((VH) holder, position);
            }
        }
    }

    public Object getLastItem() {
        if (addSectionHeaders) {
            return realmResults.get(rowWrappers.get(rowWrappers.size() - 1).realmIndex);
        } else {
            return realmResults.get(realmResults.size() - 1);
        }
    }

    @Override
    public int getItemCount() {
        int extraCount = loadMoreItem == null ? 0 : 1;

        extraCount += footerItem == null ? 0 : 1;

        if (addSectionHeaders) {
            return rowWrappers.size() + extraCount;
        }

        if (realmResults == null) {
            return extraCount;
        }
        return realmResults.size() + extraCount;
    }

    @Override
    public int getItemViewType(int position) {
        if (loadMoreItem != null && position == getItemCount() - 1) {
            return LOAD_MORE_VIEW_TYPE;
        } else if (footerItem != null && position == getItemCount() - 1) {
            return FOOTER_VIEW_TYPE;
        } else if (!rowWrappers.isEmpty() && !rowWrappers.get(position).isRealm) {
            return HEADER_VIEW_TYPE;
        }

        return getItemRealmViewType(position);
    }

    public int getItemRealmViewType(int position) {
        return super.getItemViewType(position);
    }

    /**
     * Ensure {@link #close()} is called whenever {@link Realm#close()} is called to ensure that the
     * {@link #realmResults} are invalidated and the change listener removed.
     */
    public void close() {
        updateRealmResults(null);
    }

    /**
     * Update the RealmResults associated with the Adapter. Useful when the query has been changed.
     * If the query does not change you might consider using the automaticUpdate feature.
     *
     * @param queryResults the new RealmResults coming from the new query.
     */
    public void updateRealmResults(OrderedRealmCollection<T> queryResults) {
        removeListener();
        this.realmResults = queryResults;

        realmResults = queryResults;

        addListener();

        updateRowWrappers();
        ids = getIdsOfRealmResults();

        notifyDataSetChanged();
    }

    public OrderedRealmCollection<T> getRealmResults() {
        return realmResults;
    }

    @Override
    public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        if (automaticUpdate && isDataValid()) {
            addListener();
        }
    }

    @Override
    public void onDetachedFromRecyclerView(final RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

        if (automaticUpdate && isDataValid()) {
            removeListener();
        }
    }

    private boolean isDataValid() {
        return realmResults != null && realmResults.isValid();
    }

    private void addListener() {
        if (listener != null && realmResults != null) {
            if (realmResults instanceof RealmResults) {
                RealmResults data = (RealmResults) realmResults;
                data.addChangeListener(listener);
//                TODO: how to handle realm list?
//            } else if (realmResults instanceof RealmList) {
//                RealmList data = (RealmList) realmResults;
//                data.realm.handlerController.addChangeListenerAsWeakReference(listener);
            } else {
                throw new IllegalArgumentException("RealmCollection not supported: " + realmResults.getClass());
            }
        }
    }

    private void removeListener() {
        if (listener != null && realmResults != null) {
            if (realmResults instanceof RealmResults) {
                RealmResults data = (RealmResults) realmResults;
                data.removeChangeListener(listener);
//                TODO: how to handle realm list?
//            } else if (realmResults instanceof RealmList) {
//                RealmList data = (RealmList) realmResults;
//                data.realm.handlerController.removeWeakChangeListener(listener);
            } else {
                throw new IllegalArgumentException("RealmCollection not supported: " + realmResults.getClass());
            }
        }
    }

    /**
     * Method that creates the header string that should be used. Override this method to have
     * a custom header.
     */
    public String createHeaderFromColumnValue(T result, long headerIndex) {
        Row row = getRow(result);
        RealmFieldType fieldType = row.getColumnType(headerIndex);

        Object columnValue;
        if (fieldType == RealmFieldType.STRING) {
            columnValue = row.getString(headerIndex);
        } else if (fieldType == RealmFieldType.BOOLEAN) {
            columnValue = row.getBoolean(headerIndex);
        } else if (fieldType == RealmFieldType.INTEGER) {
            columnValue = row.getLong(headerIndex);
        } else {
            throw new IllegalStateException("columnValue type not supported");
        }

        String value = null;
        if (columnValue instanceof Boolean) {
            value = columnValue.toString();
        } else if (columnValue instanceof String) {
            value = ((String) columnValue).substring(0, 1);
        } else if (columnValue instanceof Long) {
            value = columnValue.toString();
        } else {
            throw new IllegalStateException("columnType not supported");
        }

        return value;
    }

    public int findPosition(int realmIndex) {
        if (realmResults == null || realmResults.size() == 0) {
            return -1;
        }
        if (ids == null || ids.size() == 0) {
            return -1;
        }

        return ids.indexOf(getRealmRowId(realmIndex));
    }

    private List getIdsOfRealmResults() {
        if (!animateResults || realmResults == null || realmResults.size() == 0) {
            return EMPTY_LIST;
        }

        if (addSectionHeaders) {
            List ids = new ArrayList(rowWrappers.size());
            for (int i = 0; i < rowWrappers.size(); i++) {
                final RowWrapper rowWrapper = rowWrappers.get(i);
                if (rowWrapper.isRealm) {
                    ids.add(getRealmRowId(rowWrappers.get(i).realmIndex));
                } else {
                    ids.add(rowWrappers.get(i).header);
                }
            }

            return ids;
        } else {
            List ids = new ArrayList(realmResults.size());
            for (int i = 0; i < realmResults.size(); i++) {
                ids.add(getRealmRowId(i));
            }

            return ids;
        }
    }

    private Object getRealmRowId(int realmIndex) {
        Object rowPrimaryId;
        if (animatePrimaryIdType == RealmFieldType.INTEGER) {
            rowPrimaryId = getRow(realmIndex).getLong(animatePrimaryColumnIndex);
        } else if (animatePrimaryIdType == RealmFieldType.STRING) {
            rowPrimaryId = getRow(realmIndex).getString(animatePrimaryColumnIndex);
        } else {
            throw new IllegalStateException("Unknown animatedIdType");
        }

        if (animateExtraColumnIndex != -1) {
            Object value;
            if (animateExtraIdType == RealmFieldType.INTEGER) {
                value = getRow(realmIndex).getLong(animateExtraColumnIndex);
            } else if (animateExtraIdType == RealmFieldType.STRING) {
                value = getRow(realmIndex).getString(animateExtraColumnIndex);
            } else if (animateExtraIdType == RealmFieldType.DATE) {
                value = getRow(realmIndex).getDate(animateExtraColumnIndex).getTime();
            } else {
                throw new IllegalStateException("Unknown animateExtraIdType");
            }

            return String.valueOf(rowPrimaryId) + String.valueOf(value);
        } else {
            return rowPrimaryId;
        }
    }

    private Row getRow(int realmIndex) {
        T realmObject = getRealmObjectAtIndex(realmIndex);

        return getRow(realmObject);
    }

    private Row getRow(RealmModel realmObject) {
        RealmObjectProxy proxy = (RealmObjectProxy) realmObject;

        return proxy.realmGet$proxyState().getRow$realm();
    }

    private T getRealmObjectAtIndex(int realmIndex) {
        return realmResults.get(realmIndex);
    }

    private void updateRowWrappers() {
        if (realmResults == null) {
            return;
        }

        if (addSectionHeaders) {
            addSectionHeaders();
        }
    }

    private void addSectionHeaders() {
        String lastHeader = null;
        int headerCount = 0;
        int wrapperCount = 0;
        int sectionFirstPosition = 0;
        long headerIndex = headerColumnName == null ? NO_SECTION_HEADER_COLUMN : getTableOrView().getColumnIndex(headerColumnName);

        rowWrappers.clear();

        for (T result : realmResults) {
            String header = createHeaderFromColumnValue(result, headerIndex);

            if (!TextUtils.equals(lastHeader, header)) {
                sectionFirstPosition = wrapperCount + headerCount;
                lastHeader = header;
                headerCount += 1;

                rowWrappers.add(new RowWrapper(sectionFirstPosition, header));
            }

            rowWrappers.add(new RowWrapper(wrapperCount++, sectionFirstPosition));
        }
    }

    private void setIds(List ids) {
        this.ids = ids;
    }

    private RealmChangeListener<OrderedRealmCollection<T>> getRealmChangeListener() {
        return new RealmChangeListener<OrderedRealmCollection<T>>() {
            @Override
            public void onChange(OrderedRealmCollection<T> element) {
                updateRowWrappers();

                if (animateResults && ids != null && !ids.isEmpty()) {
                    List newIds = getIdsOfRealmResults();

                    // If the list is now empty, just notify the recyclerView of the change.
                    if (newIds.isEmpty()) {
                        setIds(newIds);

                        notifyDataSetChanged();
                    } else {
                        Patch patch = DiffUtils.diff(ids, newIds);
                        List<Delta> deltas = patch.getDeltas();

                        setIds(newIds);

                        if (deltas.isEmpty()) {
                            // Ids haven't changed but items themselves may have
                            notifyDataSetChanged();
                        } else if (addSectionHeaders) {
                            notifyChangeWithSectionHeaders(newIds, deltas);
                        } else {
                            notifyChange(deltas);
                        }
                    }

                } else {
                    notifyDataSetChanged();

                    setIds(getIdsOfRealmResults());
                }
            }

            private void notifyChange(List<Delta> deltas) {
                for (Delta delta : deltas) {
                    if (delta.getType() == Delta.TYPE.INSERT) {
                        notifyItemRangeInserted(
                                delta.getRevised().getPosition(),
                                delta.getRevised().size());
                    } else if (delta.getType() == Delta.TYPE.DELETE) {
                        notifyItemRangeRemoved(
                                delta.getOriginal().getPosition(),
                                delta.getOriginal().size());
                    } else {
                        notifyItemRangeChanged(
                                delta.getRevised().getPosition(),
                                delta.getRevised().size());
                    }
                }
            }

            private void notifyChangeWithSectionHeaders(List newIds, List<Delta> deltas) {
                // If sectionHeaders are enabled, the animations have some special cases and
                // the non-animated rows need to be updated as well.
                Delta delta = deltas.get(0);
                if (delta.getType() == Delta.TYPE.INSERT) {
                    if (delta.getRevised().size() == 1) {
                        notifyItemInserted(delta.getRevised().getPosition());
                    } else {
                        final Chunk revised = delta.getRevised();
                        notifyItemRangeInserted(revised.getPosition(), revised.size());
                    }
                } else if (delta.getType() == Delta.TYPE.DELETE) {
                    if (delta.getOriginal().size() == 1) {
                        notifyItemRemoved(delta.getOriginal().getPosition());
                    } else {
                        // Note: The position zero check is to hack around a indexOutOfBound
                        // exception that happens when the zero position is animated out.
                        if (delta.getOriginal().getPosition() == 0) {
                            notifyDataSetChanged();
                            return;
                        } else {
                            notifyItemRangeRemoved(
                                    delta.getOriginal().getPosition(),
                                    delta.getOriginal().size());
                        }
                    }

                    if (delta.getOriginal().getPosition() - 1 > 0) {
                        notifyItemRangeChanged(
                                0,
                                delta.getOriginal().getPosition() - 1);
                    }
                    if (delta.getOriginal().getPosition() > 0 && newIds.size() > 0) {
                        notifyItemRangeChanged(
                                delta.getOriginal().getPosition(),
                                newIds.size() - 1);
                    }
                } else {
                    notifyDataSetChanged();
                }
            }
        };
    }

    /**
     * Adds the LoadMore item.
     */
    public void addLoadMore() {
        if (loadMoreItem != null || footerItem != null) {
            return;
        }
        loadMoreItem = new Object();

        notifyDataSetChanged();
    }

    /**
     * Removes the LoadMoreItems;
     */
    public void removeLoadMore() {
        if (loadMoreItem == null) {
            return;
        }
        loadMoreItem = null;

        notifyDataSetChanged();
    }

    /**
     * Adds the Footer item.
     */
    public void addFooter() {
        if (footerItem != null || loadMoreItem != null) {
            return;
        }
        footerItem = new Object();

        notifyDataSetChanged();
    }

    /**
     * Removes the Footer;
     */
    public void removeFooter() {
        if (footerItem == null) {
            return;
        }
        footerItem = null;

        notifyDataSetChanged();
    }

    /**
     * Called when an item has been dismissed by a swipe.
     *
     * Only supported with type linearLayout and thus the realmResults can be accessed directly.
     * If it is extended to LinearLayoutWithHeaders, rowWrappers will have to be used.
     */
    public void onItemSwipedDismiss(int position) {
        BaseRealm realm = getRealm();
        realm.beginTransaction();
        RealmObject.deleteFromRealm(realmResults.get(position));
        realm.commitTransaction();
    }

    private TableOrView getTableOrView() {
        return getTableOrView(realmResults);
    }

    private TableOrView getTableOrView(OrderedRealmCollection<T> results) {
        if (results instanceof RealmList) {
            return ((RealmList)results).view.getTable();
        } else if (results instanceof RealmResults) {
            return ((RealmResults)results).getTable();
        }

        throw new IllegalStateException("Unknown OrderedRealmCollection type: " + realmResults);
    }

    private BaseRealm getRealm() {
        if (realmResults instanceof RealmList) {
            return ((RealmList)realmResults).realm;
        } else if (realmResults instanceof RealmResults) {
            return ((RealmResults)realmResults).realm;
        }

        throw new IllegalStateException("Unknown OrderedRealmCollection type: " + realmResults);
    }
}
