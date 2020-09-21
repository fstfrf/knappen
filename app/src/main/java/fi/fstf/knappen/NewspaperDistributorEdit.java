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
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Calendar;

// This is the activity where you can edit the newspaper distributors
public class NewspaperDistributorEdit extends AppCompatActivity {

    private String newspaperDistributorFolder = "";
    private NewspaperDistributor currentNewspaperDistributor = null;
    private int requestCodeForTimePicker = 201;     // This could be any value, but needs to be same on both places where checked
    private int hourFromDatePicker = 0;
    private int minuteFromDatePicker = 0;
    private int enabledFromDatePicker = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newspaper_distributor_edit);

        // Get the newspaper folder name
        Intent intent = getIntent();
        if(intent != null)
        {
            Bundle bundle = intent.getExtras();
            if (bundle != null)
            {
                newspaperDistributorFolder = bundle.getString("DISTRIBUTOR_ID_KEY");
                // intent.removeExtra("DISTRIBUTOR_ID_KEY"); Don't remove this for now
            }
        }
        else
        {
            LogDAO.getInstance().add("NewspaperDistributorEdit: no intent setup");
        }

        // Get the newspaperDistributor from the selected folder
        currentNewspaperDistributor = NewspaperDistributorDAO.getInstance().getNewspaperDistributor(newspaperDistributorFolder);

        if (newspaperDistributorFolder.isEmpty() || currentNewspaperDistributor == null)
        {
            LogDAO.getInstance().add("NewspaperDistributorEdit error, did not find newspaper distributor!");

            // Exit
            finish();
        }

        // Get all text from the database
        final EditText distributorName = (EditText)findViewById(R.id.editTextNewspaperDistrbutorName);
        distributorName.setText(currentNewspaperDistributor.getNewspaperDistributorName());

        final EditText ftpServerAddress = (EditText)findViewById(R.id.editTextFTPServerAddress);
        ftpServerAddress.setText(currentNewspaperDistributor.getFtpAddress());

        final EditText ftpPort = (EditText)findViewById(R.id.editFTPPort);
        ftpPort.setText(String.valueOf(currentNewspaperDistributor.getFtpPort()));

        final EditText ftpUsername = (EditText)findViewById(R.id.editTextUsername);
        ftpUsername.setText(currentNewspaperDistributor.getFtpUsername());

        final EditText ftpPassword = (EditText)findViewById(R.id.editTextPassword);
        ftpPassword.setText(currentNewspaperDistributor.getFtpPassword());

        Button setDownloadTimeButton = (Button)findViewById(R.id.buttonSetDownloadTime);
        setDownloadTimeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Start the NewspaperDistributorTimePicker class to be able to see the log
                Intent intent = new Intent(getApplicationContext(), NewspaperDistributorTimePicker.class);

                // Start with intent to receive result from NewspaperDistributorTimePicker
                startActivityForResult(intent, requestCodeForTimePicker);
            }
        });

        Button saveAndTestConnectionButton = (Button)findViewById(R.id.buttonSaveAndTestConnection);
        saveAndTestConnectionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String address = ftpServerAddress.getText().toString();
                String un = ftpUsername.getText().toString();
                String pw = ftpPassword.getText().toString();
                String name = distributorName.getText().toString();

                int port = 0;
                String portString = ftpPort.getText().toString();
                try {
                    port = Integer.parseInt(portString);
                } catch (NumberFormatException e) {
                    port = 21; // default
                    LogDAO.getInstance().add("Failed to set ftp port, defaulting to 21");
                }

                int maxNP = 7; // Max number of newspapers

                int hour = 0;
                int minute = 0;
                int enabled = 0;
                long interval = AlarmManager.INTERVAL_DAY;

                // We have been entering the DatePicker and saved, use those values
                if (enabledFromDatePicker == 1)
                {
                    hour = hourFromDatePicker;
                    minute = minuteFromDatePicker;
                    enabled = 1;
                }
                else if (currentNewspaperDistributor.getAutoDownloadEnabled() == 1)
                {
                    hour = currentNewspaperDistributor.getDownloadTimeHour();
                    minute = currentNewspaperDistributor.getDownloadTimeMinute();
                    enabled = currentNewspaperDistributor.getAutoDownloadEnabled();
                    //interval = currentNewspaperDistributor.getAutoDownloadInterval(); Needs testing
                }

                // Update the database
                NewspaperDistributorDAO.getInstance().updateDistributor(hour, minute, enabled, interval, address , un, pw, port, maxNP, name, newspaperDistributorFolder);

                if (enabled == 1)
                {
                    // Set the alarm if we have set the time
                    setRecurringAlarm(hour, minute, interval, newspaperDistributorFolder);
                }
                else
                {
                    Toast.makeText(NewspaperDistributorEdit.this, "Download time not yet set", Toast.LENGTH_SHORT).show();
                }

                // Make sure we use async for ftp
                new TestConnectionAsync(getApplicationContext(), newspaperDistributorFolder).execute();
            }
        });

        Button connectButton = (Button)findViewById(R.id.buttonDownloadManually);
        connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Start the Settings class to be able to see the log
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear the top, this means that also NewspaperDistributor activity will be destroyed
                intent.putExtra("DOWNLOAD_ALL_FILES_MANUALLY_FROM_DISTRIBUTOR_ID", newspaperDistributorFolder);
                startActivity(intent);
                finish();
            }
        });

        final Button deleteFiles = (Button)findViewById(R.id.buttonDeleteManually);
        deleteFiles.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Show an alert before deleting files
                showAlertDialogConfirmDeleteAllFiles();
            }
        });

        Button deleteNewspaperDistributor = (Button)findViewById(R.id.buttonDeleteNewspaperDistributor);
        deleteNewspaperDistributor.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Show alert before deleting the whole distributor + files
                showAlertDialogConfirmDeleteDistributor();
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        // Extract the data returned from the child Activity = NewspaperDistributorTimePicker.
        if (requestCode == requestCodeForTimePicker && resultCode == RESULT_OK && data != null)
        {
            TextView dlTime = (TextView)findViewById(R.id.textViewDownloadTime);
            hourFromDatePicker = data.getIntExtra("FSTF_TIME_PICKER_HOUR", 0);
            minuteFromDatePicker = data.getIntExtra("FSTF_TIME_PICKER_MINUTE", 0);
            enabledFromDatePicker = 1; // Set this manually .. now we know that we have got the data from TimePicker

            String text = String.format("%02d:%02d", hourFromDatePicker, minuteFromDatePicker);
            dlTime.setText(text);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        int hour = 0;
        int minute = 0;

        // Ok, we have changed something
        if (enabledFromDatePicker == 1)
        {
            hour = hourFromDatePicker;
            minute = minuteFromDatePicker;
        }
        else if (currentNewspaperDistributor.getAutoDownloadEnabled() == 1)
        {
            hour = currentNewspaperDistributor.getDownloadTimeHour();
            minute = currentNewspaperDistributor.getDownloadTimeMinute();
        }

        String text = "Not set";
        if(enabledFromDatePicker == 1 || currentNewspaperDistributor.getAutoDownloadEnabled() == 1)
        {
            text = String.format("%02d:%02d", hour, minute);
        }

        final TextView dlTime = (TextView)findViewById(R.id.textViewDownloadTime);
        dlTime.setText(text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // Function to display simple Alert Dialog
    public void showAlertDialogConfirmDeleteAllFiles()
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Delete all files?");
        alertDialogBuilder.setMessage("Are you sure you want to delete all downloaded files for this newspaper distributor? The Settings page will open to display the log with information.");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                // Start the Settings class to be able to see the log
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear the top, this means that also NewspaperDistributor activity will be destroyed
                intent.putExtra("DELETE_ALL_FILES_FROM_DISTRIBUTOR_ID", newspaperDistributorFolder);
                startActivity(intent);

                finish();
            }
        });
        alertDialogBuilder.setNegativeButton("CANCEL",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                // if this button is clicked, just close
                // the dialog box and do nothing
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    // Function to display simple Alert Dialog
    public void showAlertDialogConfirmDeleteDistributor()
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Delete distributor?");
        alertDialogBuilder.setMessage("Are you sure you want to delete this newspaper distributor and all files? The Settings page will open to display the log with information.");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                // Start the Settings class to be able to see the log
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear the top, this means that also NewspaperDistributor activity will be destroyed
                intent.putExtra("DELETE_DISTRIBUTOR_ID", newspaperDistributorFolder);
                startActivity(intent);
                finish();
            }
        });
        alertDialogBuilder.setNegativeButton("CANCEL",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                // if this button is clicked, just close
                // the dialog box and do nothing
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void setRecurringAlarm(int dlHour, int dlMinute, long interval, String distributorFolderID)
    {
        Context context = getApplicationContext();

        Calendar c = Calendar.getInstance();
        int currentHour = c.get(Calendar.HOUR_OF_DAY);
        int currentMinute = c.get(Calendar.MINUTE);

        c.set(Calendar.HOUR_OF_DAY, dlHour);
        c.set(Calendar.MINUTE, dlMinute);

        // This will trigger the alarm immediately and start downloading
        // Because you will not likely set this up after 24:00 at night
        // That means that the time you set is in the past (if you set 04:30 for example)
        // And all times set in past will trigger immediately
        // To avoid this we will have this check below, because this code will run every time you enter this activity

        boolean addDay = false;
        if(currentHour > dlHour)
        {
            // Case: Alarm is set to 04:30 and current time is now 16:45
            addDay = true;
        }
        else if (currentHour == dlHour)
        {
            if(currentMinute >= dlMinute)
            {
                // Case: Alarm is set to 04:30 and current time is now 04:45
                addDay = true;
            }
            else
            {
                // Case: Alarm is set to 04:30 and current time is now 04:10
                addDay = false;
            }
        }
        else
        {
            // Case: Alarm is set to 04:30 and current time is now 02:40
            addDay = false;
        }

        if(addDay)
        {
            // Add a day so the alarm will trigger this night instead of directly (night that has passed)
            // the Calendar.add function forces an immediate recomputation of the calendar's milliseconds and all fields
            c.add(Calendar.DAY_OF_MONTH, 1);
        }

        // This will trigger the alarm immediately and start downloading
        // Because you will not likely set this up after 24:00 at night
        // That means that the time you set is in the past (if you set 04:30 for example)
        // And all times set in past will trigger immediately

        Intent downloader = new Intent(context, AlarmReceiver.class);
        downloader.putExtra("NEWSPAPER_DISTRIBUTOR_FOLDER_ID", distributorFolderID);

        int requestCode = 0;
        try
        {
            requestCode = Integer.valueOf(distributorFolderID);
        }
        catch (NumberFormatException e)
        {
            LogDAO.getInstance().add("NewspaperDistributorEdit::setRecurringAlarm() - Could not convert id to integer!");
        }

        PendingIntent recurringDownload = PendingIntent.getBroadcast(context,requestCode, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarms.setRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), interval, recurringDownload);

        String text = String.format("Alarm download time set to %02d:%02d", dlHour, dlMinute);
        LogDAO.getInstance().add(text);
    }
}

