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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;

// This is our custom LogAdapter class (BaseAdapter is the base class) that will write database logs
public class LogAdapter extends BaseAdapter {

    // Good source used in several projects
    // https://www.survivingwithandroid.com/2014/08/android-listview-with-multiple-row.html

    private List<LogItem> logItemList;
    private Context context;

    public LogAdapter(List<LogItem> mList, Context context)
    {
        super();
        this.context = context;
        this.logItemList = mList;
    }

    @Override
    public int getCount()
    {
        return logItemList.size();
    }

    @Override
    public Object getItem(int position)
    {
        // This function is never used!
        return position;
    }

    @Override
    public long getItemId(int position)
    {
        // This function will be called when you tap on the item in list
        // First in list = 0, second = 1 etc
        return position;
    }

    @Override
    public int getViewTypeCount()
    {
        // For now we only have 1 type of log items
        return 1;
    }

    public LogItem getLogItem(int position)
    {
        if ( !logItemList.isEmpty())
            return logItemList.get(position);
        else
            return null;
    }

    @Override
    public int getItemViewType(int position)
    {
        // We only have one type for now
        return 1;
    }

    // The view holder class that explains how one of the log item views should look like
    private class LogItemViewHolder
    {
        // Good source used in several projects
        // https://developer.android.com/training/improving-layouts/smooth-scrolling.html

        TextView    timestamp;
        TextView    text;

        LogItemViewHolder()
        {
            timestamp = null;
            text = null;
        }
    }

    // This function will automatically be called on create, update etc ... here we create the whole list
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LogItemViewHolder holder;
        LogItem message = logItemList.get(position);

        if (convertView == null)
        {
            // Inflate the layout according to the view type
            // In our case here we only have one type
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            // LOG ITEMS
            if(inflater != null)
                convertView = inflater.inflate(R.layout.log_item_list_layout, parent, false);
            else
                return null;

            // Here we could have other types of items in log

            // Setup the view holder
            holder = new LogItemViewHolder();
            holder.timestamp    = (TextView) convertView.findViewById(R.id.textViewTimestamp);
            holder.text         = (TextView) convertView.findViewById(R.id.textViewText);

            // Save the holder
            convertView.setTag(holder);
        }
        else
            holder = (LogItemViewHolder)convertView.getTag();


        // Date
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        String stringNow = sdf.format(message.getTimestamp());
        holder.timestamp.setText(stringNow);

        // Log item text
        holder.text.setText(message.getText());

        return convertView;
    }

    public void emptyListManually()
    {
        logItemList.clear();
        updateList();
    }

    public void addLogItem(LogItem item)
    {
        logItemList.add(item);
        updateList();
    }

    public void updateList()
    {
        // This needs to be called from UI thread
        this.notifyDataSetChanged();
    }
}
