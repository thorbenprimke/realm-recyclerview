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
import android.view.LayoutInflater;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import co.moonmonkeylabs.realmrecyclerview.DiffableLongIdItem;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

public abstract class RealmBasedRecyclerViewAdapter<T extends RealmObject,
        VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private static final List<Long> EMPTY_LIST = new ArrayList<>(0);

    protected LayoutInflater inflater;
    protected RealmResults<T> realmResults;
    protected List<Long> ids;

    private final RealmChangeListener listener;

    public RealmBasedRecyclerViewAdapter(
            Context context,
            RealmResults<T> realmResults,
            boolean automaticUpdate) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.inflater = LayoutInflater.from(context);
        this.listener = (!automaticUpdate) ? null : new RealmChangeListener() {
            @Override
            public void onChange() {
                if (ids != null && !ids.isEmpty()) {
                    List<Long> newIds = getIdsOfRealmResults();
                    Patch patch = DiffUtils.diff(ids, newIds);
                    List<Delta> deltas = patch.getDeltas();
                    ids = newIds;
                    if (deltas.isEmpty()) {
                        // Nothing has changed - most likely because the notification was for
                        // a different object/table
                    } else if (deltas.size() > 5) {
                        notifyDataSetChanged();
                    } else {
                        for (Delta delta: deltas) {
                            if (delta.getType() == Delta.TYPE.INSERT) {
                                notifyItemInserted(delta.getRevised().getPosition());
                            } else if (delta.getType() == Delta.TYPE.DELETE) {
                                notifyItemRemoved(delta.getOriginal().getPosition());
                            }
                        }
                    }
                } else {
                    notifyDataSetChanged();
                    ids = getIdsOfRealmResults();
                }
            }
        };
        updateRealmResults(realmResults);
    }

    private List<Long> getIdsOfRealmResults() {
        if (realmResults.size() == 0 || !(realmResults.get(0) instanceof DiffableLongIdItem)) {
            return EMPTY_LIST;
        }
        List<Long> ids = new ArrayList<>(realmResults.size());
        Iterator<T> iterator = realmResults.iterator();
        while (iterator.hasNext()) {
            ids.add(((DiffableLongIdItem) iterator.next()).getId());
        }
        return ids;
    }

    @Override
    public int getItemCount() {
        if (realmResults == null) {
            return 0;
        }
        return realmResults.size();
    }

    /**
     * Update the RealmResults associated to the Adapter. Useful when the query has been changed.
     * If the query does not change you might consider using the automaticUpdate feature
     *
     * @param queryResults the new RealmResults coming from the new query.
     */
    public void updateRealmResults(RealmResults<T> queryResults) {
        if (listener != null) {
            if (this.realmResults != null) {
                this.realmResults.getRealm().removeChangeListener(listener);
            }
            if (queryResults != null) {
                queryResults.getRealm().addChangeListener(listener);
            }
        }

        this.realmResults = queryResults;
        ids = getIdsOfRealmResults();
        notifyDataSetChanged();
    }
}

