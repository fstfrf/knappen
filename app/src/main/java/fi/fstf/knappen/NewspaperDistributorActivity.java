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

import android.app.AlarmManager;
import android.app.ExpandableListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import fi.fstf.knappen.helper.OnStartDragListener;
import fi.fstf.knappen.helper.SimpleItemTouchHelperCallback;

import java.util.List;

// This is the activity used for showing the list of NewspaperDistributors (ÖT, VBL, Kyrkpressen etc)
public class NewspaperDistributorActivity extends AppCompatActivity implements OnStartDragListener {

    private List<NewspaperDistributor>  listFromDB = null;
    private RecyclerView recyclerView;
    private NewspaperDistributorRecycleViewAdapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ItemTouchHelper mItemTouchHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_newspaper_distributor);

        // Get all items from DB
        listFromDB = NewspaperDistributorDAO.getInstance().getAllNewspaperDistributorsSorted();

        if (listFromDB != null && listFromDB.size() == 0)
        {
            // First time -> create the only newspaper we have for now
            NewspaperDistributorDAO.getInstance().add(0, 0, 0, AlarmManager.INTERVAL_DAY,"", "", "",21, 7, "Click to setup", "01", 1);

            // Get the list again
            listFromDB = NewspaperDistributorDAO.getInstance().getAllNewspaperDistributorsSorted();
        }

        // RECYCLER VIEW - WITH DRAGGABLE ITEMS
        // https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-b9456d2b1aaf
        // https://developer.android.com/guide/topics/ui/layout/recyclerview

        recyclerView = (RecyclerView) findViewById(R.id.listViewNewspaperDistributorsRecyclerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        mAdapter = new NewspaperDistributorRecycleViewAdapter(listFromDB, this);
        recyclerView.setAdapter(mAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);


        mAdapter.setOnItemClickListener(new NewspaperDistributorRecycleViewAdapter.ClickListener() {
                @Override
                public void onItemClick(int position, View v) {
                    NewspaperDistributor clickedDistributor = mAdapter.getNewspaperDistributor(position);
                    Intent intent = new Intent(getApplicationContext(), NewspaperDistributorEdit.class);
                    intent.putExtra("DISTRIBUTOR_ID_KEY", clickedDistributor.getFolderName());
                    startActivity(intent);
                    return;
                }
            });

        // TOOLBAR & ACTIONBAR
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar bar = getSupportActionBar();
        if ( bar != null)
        {
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settingsmenu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // User chose the "back" item
                finish();
                return true;

            case R.id.action_add:

                String folderStringID = "";
                int nextID = NewspaperDistributorDAO.getInstance().getNextID();
                try
                {
                    if (nextID < 10)
                        folderStringID = "0" + String.valueOf(nextID);
                    else
                        folderStringID = String.valueOf(nextID);
                }
                catch (Exception e)
                {
                    folderStringID = "99";
                    LogDAO.getInstance().add("Failed to get next Newspaperdistributor ID");
                }

                // User chose the "add" action, add a new newspaperdistributor
                NewspaperDistributorDAO.getInstance().add(0, 0, 0, AlarmManager.INTERVAL_DAY,"", "", "",21, 7, "Click to setup", folderStringID, nextID);

                // Refresh list
                reCreateListAdapter();

                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sp_shared_preferences), Context.MODE_PRIVATE);
        boolean shutDownApp = sharedPref.getBoolean(getString(R.string.sp_shutdown_application), false); // false = default

        if (shutDownApp == true)
            finish();

        // Refresh list
        reCreateListAdapter();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void reCreateListAdapter()
    {
        // Empty the list
        mAdapter.emptyListManually();

        // Get all DB messages
        listFromDB = NewspaperDistributorDAO.getInstance().getAllNewspaperDistributorsSorted();

        // Loop through list and add them manually (we don't want to create a new list adapter
        for (int i=0; i<listFromDB.size(); i++) {
            mAdapter.add(listFromDB.get(i));
        }

        // Make sure to notify that the list is updated
        mAdapter.updateList();
    }

}
