/*
 * Copyright 2020 Finlands svenska taltidningsförening rf.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *     */
package fi.fstf.knappen;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import fi.fstf.knappen.helper.ItemTouchHelperAdapter;
import fi.fstf.knappen.helper.ItemTouchHelperViewHolder;
import fi.fstf.knappen.helper.OnStartDragListener;

import java.util.Collections;
import java.util.List;

public class NewspaperDistributorRecycleViewAdapter extends RecyclerView.Adapter<NewspaperDistributorRecycleViewAdapter.NewspaperDistributorRecyclerViewHolder> implements ItemTouchHelperAdapter
{
    private List<NewspaperDistributor> mList;
    private final OnStartDragListener mDragStartListener;
    private static ClickListener mClickListener;

    // Provide a suitable constructor
    public NewspaperDistributorRecycleViewAdapter(List<NewspaperDistributor> list, OnStartDragListener dragListener)
    {
        super();
        mList = list;
        mDragStartListener = dragListener;
    }

    // Provide a reference to the views for each data item
    public static class NewspaperDistributorRecyclerViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder, View.OnClickListener {

        TextView    mName;
        ImageView   mImage;
        ImageView   mSwipeControllerImage;

        public NewspaperDistributorRecyclerViewHolder(View itemView) {

            super(itemView); // The whole view

            itemView.setOnClickListener(this);

            mName = (TextView)itemView.findViewById(R.id.textViewName);
            mImage = (ImageView)itemView.findViewById(R.id.distributorImage);
            mSwipeControllerImage = (ImageView)itemView.findViewById(R.id.swipeControllerImage);
        }

        @Override
        public void onClick(View v) {
            // This is from implements View.OnClickListener
            mClickListener.onItemClick(getAdapterPosition(), v);
        }

        @Override
        public void onItemSelected()
        {
            // This is automatically called from implements ItemTouchHelperViewHolder
            itemView.setBackgroundColor(Color.LTGRAY);
        }

        @Override
        public void onItemClear()
        {
            // This is automatically called from implements ItemTouchHelperViewHolder
            itemView.setBackgroundColor(0);
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public NewspaperDistributorRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // create a new view
        //TextView v = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.my_text_view, parent, false);

        View itemView = null;
        Context cx = parent.getContext();

        if (cx == null)
            return null;

        // Inflate the layout according to the view type
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // LOG ITEMS
        if (inflater != null)
            itemView = inflater.inflate(R.layout.newspaper_distributor_list_layout, parent, false);
        else
            return null;

        NewspaperDistributorRecyclerViewHolder vh = new NewspaperDistributorRecyclerViewHolder(itemView);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final NewspaperDistributorRecyclerViewHolder holder, int position) {

        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.mName.setText(mList.get(position).getNewspaperDistributorName()); // + " - sortID : " + Integer.toString(mList.get(position).getSortID()));
        //holder.mImage.setImageResource(R.drawable.ic_tap_and_play_black_24dp);

        // Start a drag whenever the handle view it touched
        holder.mSwipeControllerImage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mDragStartListener.onStartDrag(holder);
                }
                return false;
            }
        });
/*
        holder.mName.setOnClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id)
            {
                NewspaperDistributor clickedDistributor = null;
                if ( !mList.isEmpty())
                {
                    clickedDistributor = mList.get(position);
                }

                if (clickedDistributor != null)
                {
                    Context cx = v.getContext();
                    if (cx!=null)
                    {
                        Intent intent = new Intent(cx, NewspaperDistributorEdit.class);
                        intent.putExtra("DISTRIBUTOR_ID_KEY", clickedDistributor.getFolderName());
                        cx.startActivity(intent);
                    }
                }
            }
        });
*/
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mList.size();
    }

    public void emptyListManually()
    {
        mList.clear();
        updateList();
    }

    @Override
    public void onItemDismiss(int position) {
        // If we swipe to delete the item (disabled for now)
        //mList.remove(position);
        //notifyItemRemoved(position);
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(mList, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);

        // TODO - Here we shall update sorting
        NewspaperDistributor fromDist = getNewspaperDistributor(fromPosition);
        NewspaperDistributor toDist = getNewspaperDistributor(toPosition);

        // Update list
        int temp = fromDist.getSortID();
        fromDist.setSortID(toDist.getSortID());
        toDist.setSortID(temp);

        // Also update DB
        NewspaperDistributorDAO.getInstance().updateDistributorSorting(fromDist.getFolderName(), fromDist.getSortID());
        NewspaperDistributorDAO.getInstance().updateDistributorSorting(toDist.getFolderName(), toDist.getSortID());

        //updateList();

        return true;
    }

    public void add(NewspaperDistributor item)
    {
        mList.add(item);
        updateList();
    }

    public void updateList()
    {
        // This needs to be called from UI thread
        this.notifyDataSetChanged();
    }

    public NewspaperDistributor getNewspaperDistributor(int position)
    {
        if ( !mList.isEmpty())
            return mList.get(position);
        else
            return null;
    }


    public void setOnItemClickListener(ClickListener clickListener) {
        NewspaperDistributorRecycleViewAdapter.mClickListener = clickListener;
    }

    public interface ClickListener {
        void onItemClick(int position, View v);
        //void onItemLongClick(int position, View v);
    }
}
