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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

// This is the settings activity where you can change all settings + check the log
public class SettingsActivity extends AppCompatActivity {

    private List<LogItem>   logItemListFromDB = null;
    private LogAdapter      logAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Get all log items from DB
        logItemListFromDB = LogDAO.getInstance().getAllLogItems();

        // Populate our custom List adapter
        logAdapter = new LogAdapter(logItemListFromDB, this);
        ListView lv = (ListView) findViewById(R.id.listViewLogList);
        lv.setAdapter(logAdapter);

        // EXIT BUTTON
        Button exitButton = (Button)findViewById(R.id.exitButton);
        exitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MediaPlayerManager.getInstance().stop();
                MediaPlayerManager.getInstance().setEnabled(false);

                // Once you finish this activity, you will enter the main activity. Make sure to set the boolean
                SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sp_shared_preferences), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.sp_shutdown_application), true);
                editor.commit();

                finish();   // Exit Activity -> Exit application
            }
        });

        // ENABLE TOUCHSCREEN PLAYER BUTTON
        Button enableTouchScreenPlayer = (Button)findViewById(R.id.enableTouchScreenPlayer);
        enableTouchScreenPlayer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Close this activity
                finish();
            }
        });

        // NEWSPAPER SETTINGS BUTTON
        Button newspaperSettingsButton = (Button)findViewById(R.id.newspaperSettingsButton);
        newspaperSettingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Go to the Newspaper Distributor List activity
                Context context = getApplicationContext();
                Intent pushIntent = new Intent(context, NewspaperDistributorActivity.class);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) // API level below 24 need this to work
                    pushIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP); // Create a new task

                context.startActivity(pushIntent);

            }
        });

        // EMPTY LOG BUTTON
        Button emptyLogButton = (Button)findViewById(R.id.emptyLogButton);
        emptyLogButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Delete everything in the log and refresh list
                LogDAO.getInstance().deleteAllLogItems();
                reCreateListAdapter();
            }
        });

        // SHOW APP INFORMATION BUTTON
        Button showAppInformationButton = (Button)findViewById(R.id.showAppInformationButton);
        showAppInformationButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Go to the Information activity
                Context context = getApplicationContext();
                Intent pushIntent = new Intent(context, InformationActivity.class);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) // API level below 24 need this to work
                    pushIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear top = only one activity

                context.startActivity(pushIntent);
            }
        });

        // SEEK BAR AND TEXT VALUE
        TextView seekBarValue = (TextView)findViewById(R.id.speedSeekBarValue);

        // Get the saved values
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sp_shared_preferences), Context.MODE_PRIVATE);
        float audio_speed = sharedPref.getFloat(getString(R.string.sp_audio_speed), 1.0f); // 1.0f = default

        // Change these values if you like to
        float speedMax          = 2.4f;     // The speed multiplier max value
        final float speedMin    = 0.6f;     // The speed multiplier min value
        //float speedStep         = 0.2f;     // The value you will increase or decrease the multiplier for each step on the seek bar

        int seekBarValueMax     = (int)(100.0f * (speedMax - speedMin));
        float currentValue      = (speedMax - audio_speed) * 100.0f;
        int progress            = seekBarValueMax - (int)currentValue;

        // Progress will go from 0 - seekBarValueMax
        // With values max = 2.4 and min = 0.6, the progress should be 40 if speed is 1.0, and 180 if speed is 2.4, and 0 if speed is 0.6

        // Set the initial value here
        String speedText = String.format("%.1f", audio_speed);
        seekBarValue.setText(speedText);

        SeekBar seekBar = (SeekBar) findViewById(R.id.speedSeekBar);
        seekBar.setMax(seekBarValueMax);
        seekBar.setProgress((int)progress);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                float speed = (float)seekBar.getProgress() * 0.01f + speedMin;
                MediaPlayerManager.getInstance().setSpeed(speed, false);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                // This is where the step size is set
                progress = progress / 10;
                progress = progress * 10;

                float speed = (float)progress * 0.01f + speedMin;
                String speedText = String.format("%.1f", speed);

                TextView sbValue = (TextView)findViewById(R.id.speedSeekBarValue);
                sbValue.setText(speedText);

                seekBar.setProgress(progress);
            }
        });

        // Register custom Broadcast receiver to show messages on activity
        LocalBroadcastManager.getInstance(this).registerReceiver(mHandleMessageReceiver, new IntentFilter(getString(R.string.broadcastUpdateUIToSettingsActivity)));
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sp_shared_preferences),Context.MODE_PRIVATE);
        boolean shutDownApp = sharedPref.getBoolean(getString(R.string.sp_shutdown_application), false); // false = default

        if (shutDownApp)
            finish();

        // Refresh list
        reCreateListAdapter();

        // Check if we started this intent from the NewspaperDistributorEdit class
        Intent intent = getIntent();
        if(intent != null)
        {
            Bundle bundle = intent.getExtras();
            if (bundle != null)
            {
                String deleteFilesAndFoldersFromDistributorID = bundle.getString("DELETE_ALL_FILES_FROM_DISTRIBUTOR_ID");
                if ( deleteFilesAndFoldersFromDistributorID != null && !deleteFilesAndFoldersFromDistributorID.isEmpty())
                {
                    // Make sure we use async
                    new DeleteFilesAsync(SettingsActivity.this, false, deleteFilesAndFoldersFromDistributorID).execute();

                    // Remember to delete the intent, else it will run again if screen is turned sideways etc. (each onResume or onCreate)
                    intent.removeExtra("DELETE_ALL_FILES_FROM_DISTRIBUTOR_ID");
                }

                String deleteNewspaperDistributorID = bundle.getString("DELETE_DISTRIBUTOR_ID");
                if ( deleteNewspaperDistributorID != null && !deleteNewspaperDistributorID.isEmpty()) {

                    // THIS WILL DELETE ALL FILES + THE DISTRIBUTOR

                    // Make sure we use async
                    new DeleteFilesAsync(SettingsActivity.this, true, deleteNewspaperDistributorID).execute();

                    // Remember to delete the intent, else it will run again if screen is turned sideways etc. (each onResume or onCreate)
                    intent.removeExtra("DELETE_DISTRIBUTOR_ID");
                }

                String downloadAllFilesManuallyForDistributorID = bundle.getString("DOWNLOAD_ALL_FILES_MANUALLY_FROM_DISTRIBUTOR_ID");
                if (downloadAllFilesManuallyForDistributorID != null && !downloadAllFilesManuallyForDistributorID.isEmpty())
                {
                    // Make sure we use async for ftp
                    new DownloadFilesAsync(SettingsActivity.this, getApplicationContext(), downloadAllFilesManuallyForDistributorID, false).execute();

                    // Remember to delete the intent, else it will run again if screen is turned sideways etc. (each onResume or onCreate)
                    intent.removeExtra("DOWNLOAD_ALL_FILES_MANUALLY_FROM_DISTRIBUTOR_ID");
                }

                // Refresh list again
                reCreateListAdapter();
            }
        }
    }

    @Override
    protected void onDestroy() {
        try {
            // Unregister Broadcast Receiver
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mHandleMessageReceiver);

        } catch (Exception e) {
            LogDAO.getInstance().add(e.getMessage());
        }
        super.onDestroy();
    }

    // Create a broadcast receiver for Settings Activity to get handle things from async tasks
    private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            // This will run when MainActivity has got the message from another class (could be async)
            Boolean refreshLog = false;
            Bundle extras = intent.getExtras();
            if (extras != null)
            {
                refreshLog = extras.getBoolean("FSTF_UpdateLog_KEY");
            }

            if (refreshLog)
                reCreateListAdapter();
        }
    };

    private void reCreateListAdapter()
    {
        // Empty the list
        logAdapter.emptyListManually();

        // Get all DB messages
        logItemListFromDB = LogDAO.getInstance().getAllLogItems();

        // Loop through list and add them manually (we don't want to create a new list adapter
        for (int i=0; i<logItemListFromDB.size(); i++) {
            logAdapter.addLogItem(logItemListFromDB.get(i));
        }

        // Make sure to notify that the list is updated
        logAdapter.updateList();
    }
}
