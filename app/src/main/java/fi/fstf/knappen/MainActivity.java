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

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static int      touchCounter = 0;
    private static int      lastTouchedView = 4; // Start with 4 to make possible to continue with view 1
    private static Timer    nextArticleTimer = null;
    private static Timer    previousArticleTimer = null;
    private static Timer    previousNewspaperTimer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Make the window have the lowest brightness level
        // You don't need the Settings permission to do it this way
        WindowManager.LayoutParams lp = this.getWindow().getAttributes();
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF; //0.00001f;  // dim the display
        this.getWindow().setAttributes(lp);

        // Create the database
        LogDAO.getInstance().createDB(this);

        // Create the Newspaper Distributor
        NewspaperDistributorDAO.getInstance().createDB(this);
        FileManager.getInstance().setApplicationContext(getApplicationContext());

        // Update to find newest date folder
        FileManager.getInstance().updateToNewestPlayableFolder();

        // Loop through all newspaper (sorted) and setup the one with most prio
        List<NewspaperDistributor> sortedList = NewspaperDistributorDAO.getInstance().getAllNewspaperDistributorsSorted();
        if (sortedList.size() > 0)
        {
            String highestPrioNewspaper = sortedList.get(0).getFolderName();

            // Get the most prioritized newspaper and set that one for playback
            FileManager.getInstance().gotoFirstPlayableNewspaperInCurrentDate();

            // This function will update local file lists (needed for file comparison before starting to download), will be run several times later also
            FileManager.getInstance().updateLocalFileLists(highestPrioNewspaper);
        }


        // Setup the MediaPlayerManager and activate it
        MediaPlayerManager.getInstance().setApplicationContext(getApplicationContext());
        MediaPlayerManager.getInstance().setEnabled(true);
        MediaPlayerManager.getInstance().setShouldPauseAfterCompletion(false);

        // Get permission to record audio
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String requiredPermission = Manifest.permission.RECORD_AUDIO;

            // If the user previously denied this permission then show a message explaining why this permission is needed
            if (checkCallingOrSelfPermission(requiredPermission) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{requiredPermission}, 101);
            }
        }

        // Register custom Broadcast receiver for MainActivity to start playing welcome from async task when phone is rebooted
        LocalBroadcastManager.getInstance(this).registerReceiver(mHandleMessageReceiverMainActivity, new IntentFilter(getString(R.string.broadcastToMainActivity)));

        touchCounter = 0;

        // Fullscreentouchview is the whole view that you can touch
        View fullScreenTouchView = (View)findViewById(R.id.fullScreenTouchView);
        fullScreenTouchView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                int action = event.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:

                        // If the touch will be held for 3 seconds, trigger voice to play and after that go to next newspaper
                        if(nextArticleTimer==null)
                        {
                            nextArticleTimer = new Timer();
                            nextArticleTimer.schedule(new NextArticleTimerCallback(), 3000);
                            // This will not start playing the article, only the Eva voice
                        }
                        if(previousArticleTimer==null)
                        {
                            previousArticleTimer = new Timer();
                            previousArticleTimer.schedule(new PreviousArticleTimerCallback(), 6000);
                            // This will not start playing the article, only the Eva voice
                        }
                        if(previousNewspaperTimer==null)
                        {
                            previousNewspaperTimer = new Timer();
                            previousNewspaperTimer.schedule(new PreviousNewspaperTimerCallback(), 10000);
                            // This will start playing Eva "previous newspaper" + robot voice
                        }

                        break;
                    case MotionEvent.ACTION_UP:

                        long eventDuration = event.getEventTime() - event.getDownTime();
                        //String text = String.format("Pressed %d milliseconds\n", eventDuration);
                        //LogDAO.getInstance().add(text);


                        if (eventDuration < 200)
                        {
                            // Do nothing
                        }
                        else if (eventDuration < 3000)
                        {
                            // Toggle between play or pause
                            MediaPlayerManager.getInstance().togglePlayPause();
                        }
                        else if (eventDuration < 10000 ) // The same code will be run for <6000 = next article AND <10000 = previous article
                        {
                            // Special case, this is where we actually start playing the next article or previous article
                            MediaPlayerManager.getInstance().playReaderFiles();
                        }


                        // Always cancel timer when touch is released, and make sure to null it
                        if(nextArticleTimer!=null)
                        {
                            nextArticleTimer.cancel();
                            nextArticleTimer = null;
                        }
                        if(previousArticleTimer!=null)
                        {
                            previousArticleTimer.cancel();
                            previousArticleTimer = null;
                        }
                        if(previousNewspaperTimer!=null)
                        {
                            previousNewspaperTimer.cancel();
                            previousNewspaperTimer = null;
                        }

                        break;
                }

                return true;
            }
        });

        // We have 4 corner touch views that needs to be held down to enter Settings
        View cornerTouchView1 = (View)findViewById(R.id.cornerTouchView1);
        cornerTouchView1.setClipToOutline(true);
        cornerTouchView1.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int action = event.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                    {
                        // Check if we have pressed on view number 4
                        if (lastTouchedView == 4)
                            touchCounter++;
                        else
                            touchCounter = 1; // Special case, you can always start with view number 1

                        // Always set last touched
                        lastTouchedView = 1;
                        break;
                    }
                }

                checkEnterMenu();
                return false;
            }
        });

        View cornerTouchView2 = (View)findViewById(R.id.cornerTouchView2);
        cornerTouchView2.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int action = event.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                    {
                        if(lastTouchedView == 1 && touchCounter != 0)
                            touchCounter++;
                        else
                            touchCounter = 0;

                        // Always set last touched
                        lastTouchedView = 2;
                        break;
                    }
                }

                checkEnterMenu();
                return false;
            }
        });

        View cornerTouchView3 = (View)findViewById(R.id.cornerTouchView3);
        cornerTouchView3.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int action = event.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN: // Now checking when user releases finger
                    {
                        if(lastTouchedView == 2 && touchCounter != 0)
                            touchCounter++;
                        else
                            touchCounter = 0;

                        // Always set last touched
                        lastTouchedView = 3;
                        break;
                    }

                }
                checkEnterMenu();
                return false;
            }
        });

        View cornerTouchView4 = (View)findViewById(R.id.cornerTouchView4);
        cornerTouchView4.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int action = event.getActionMasked();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                    {
                        if(lastTouchedView == 3 && touchCounter != 0)
                            touchCounter++;
                        else
                            touchCounter = 0;

                        // Always set last touched
                        lastTouchedView = 4;
                        break;
                    }
                }

                checkEnterMenu();
                return false;
            }
        });

        // First time the application is run these values will be set to default.
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sp_shared_preferences), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.sp_shutdown_application), false); // false as default
        editor.commit();

        // Setup the recurring alarm on start
        setRecurringAlarm();

        // Set the text on main screen
        TextView infoTextView = (TextView)findViewById(R.id.infoTextView);
        writeTextToMainScreen(infoTextView);
    }

    public void writeTextToMainScreen(TextView tw)
    {
        if(tw!=null)
        {
            String text = "\n\nTryck på skärmen:\n\n0 - 3 s.\n= Pausar/fortsätter\n\n3 - 6 s.\n= Nästa artikel\n\n6 - 10 s.\n= Föregående artikel\n\nmer än 10 s.\n= Föregående tidning";

            //SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sp_shared_preferences), Context.MODE_PRIVATE);
            //String rebootTime = sharedPref.getString(getString(R.string.sp_reboot_time), "");
            tw.setText(text );//+ "\n\n\nSenaste omstart:\n" + rebootTime);
        }
    }

    // This function will be run from OnBootBroadcastReceiver now when we have FLAG_ACTIVITY_SINGLE_TOP as flag
    // This function will NOT be run if we don't have home button set to FSTF app.
    // If home button is set to FSTF when rebooted, the app will automatically start by Android -> home
    // After that, the application will receive OnBootBroadcastReceiver to start, and the app will be on top, and therefore launch this function
    /*
    @Override
    public void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        // Don't do stuff here, it will go to onResume where you can get the intent by getIntent()
        setIntent(intent);
    }*/


    // Create a broadcast receiver for MainActivity to get handle things from async tasks
    private final BroadcastReceiver mHandleMessageReceiverMainActivity = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            // This will run when MainActivity has got the message from another class (could be async)
            Bundle extras = intent.getExtras();
            if (extras != null)
            {
                boolean playWelcome = extras.getBoolean("FSTF_PlayWelcome_KEY");
                if (playWelcome)
                {
                    // Play welcome message, but pause playing newspaper after that
                    MediaPlayerManager.getInstance().playVoiceMessage(VoiceID.VID_WELCOME_TO_THE_NEWSPAPER.ordinal(), true);

                    // Also make sure to remove it to not be triggered again
                    intent.removeExtra("FSTF_PlayWelcome_KEY");
                }
                /*
                boolean playNewspaperLoading = extras.getBoolean("FSTF_PlayNewspaperLoading_KEY");
                if (playNewspaperLoading)
                {
                    // Play welcome message, but pause playing newspaper after that
                    MediaPlayerManager.getInstance().playVoiceMessage(VoiceID.VID_ONE_MOMENT_NEWSPAPER_LOADING.ordinal(), true);

                    // Also make sure to remove it to not be triggered again
                    intent.removeExtra("FSTF_PlayNewspaperLoading_KEY");
                }
                */
            }

        }
    };

    // This is the timer task that will be called after x seconds when touch screen is hold
    class NextArticleTimerCallback extends TimerTask {

        @Override
        public void run()
        {
            // This will be run after timer is up
            MediaPlayerManager.getInstance().playNextArticle(true);
        }
    }
    class PreviousArticleTimerCallback extends TimerTask {

        @Override
        public void run()
        {
            // This will be run after timer is up

            // This one is needed because playNextArticle() has run before this and set the current file to +1.
            // Now the counter will be back on the file currently playing in background
            FileManager.getInstance().gotoPreviousPlayableFile();

            // Play the Eva voice and pause after that
            MediaPlayerManager.getInstance().playPreviousArticle(true);
        }
    }
    class PreviousNewspaperTimerCallback extends TimerTask {

        @Override
        public void run()
        {
            // This will be run after timer is up
            MediaPlayerManager.getInstance().playPreviousNewspaper();
        }
    }
    /*
    class NextNewspaperTimerCallback extends TimerTask {

        @Override
        public void run()
        {
            // This will be run after timer is up
            MediaPlayerManager.getInstance().playNextNewspaper();
        }
    }
    */
    public void checkEnterMenu()
    {
        if (touchCounter < 0)   // Just a safety check if some weird touch sequence happens
            touchCounter = 0;

        // This is to be able to test the click during debug on emulator, it will be enough to click in one corner
        if(BuildConfig.DEBUG)
        {
            if (touchCounter >= 4)
                touchCounter = 12;
        }

        // Check if we have pressed 3 rounds of 4 views
        if( touchCounter >= 12)
        {
            touchCounter = 0; // Make sure to reset

            // Always cancel timer, and make sure to null it
            if(nextArticleTimer!=null)
            {
                nextArticleTimer.cancel();
                nextArticleTimer = null;
            }
            if(previousArticleTimer!=null)
            {
                previousArticleTimer.cancel();
                previousArticleTimer = null;
            }
            if(previousNewspaperTimer!=null)
            {
                previousNewspaperTimer.cancel();
                previousNewspaperTimer = null;
            }

            // Stop the playing of media
            if(MediaPlayerManager.getInstance().isPlaying())
                MediaPlayerManager.getInstance().togglePlayPause();

            // This will force the manager to not play any more voices after stopped
            MediaPlayerManager.getInstance().setEnabled(false);

            // Go to settings activity
            Context context = getApplicationContext();
            Intent pushIntent = new Intent(context, SettingsActivity.class);
            pushIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP); // This will create a new task - do it always here (will work with <24 android versions)

            context.startActivity(pushIntent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check if we are getting here from onNewIntent()
        // You will also get an intent every time onResume is called
        Intent intent = getIntent();
        if(intent != null)
        {
            // Check if it is the first start after reboot
            boolean firstStartAfterReboot = false;
            Bundle extras = intent.getExtras();
            if (extras != null)
            {
                // Check if we have the extra data in the intent (sent from OnBootBroadcastReceiver)
                firstStartAfterReboot = extras.getBoolean("FSTF_DOWNLOAD_FILES_ON_BOOTUP_KEY");

                // Make sure we remove the extra, else it will go here every time MainActivity is started
                intent.removeExtra("FSTF_DOWNLOAD_FILES_ON_BOOTUP_KEY");
            }

            if(firstStartAfterReboot)
            {
                LogDAO.getInstance().add("First start after reboot...");

                // Inform the user that we are loading/downloading...
                //MediaPlayerManager.getInstance().playVoiceMessage(VoiceID.VID_ONE_MOMENT_NEWSPAPER_LOADING.ordinal(), true);

                List<NewspaperDistributor> listFromDB = NewspaperDistributorDAO.getInstance().getAllNewspaperDistributorsSorted();
                if (listFromDB != null)
                {
                    boolean startPlayingWelcomeForThisNewspaper = true;
                    for( int listLoop = 0; listLoop < listFromDB.size(); listLoop++)
                    {
                        String distributorFolderID = listFromDB.get(listLoop).getFolderName();

                        // Start downloading and play the welcome message later when files has started to download
                        // The activity will be set here, if someone needs to follow the log and see it updating (after restart)
                        // These files will not be downloaded at the same time, the ASYNC class is made to wait for the first to complete before starting with next one
                        new DownloadFilesAsync(this, this, distributorFolderID, startPlayingWelcomeForThisNewspaper).execute();

                        LogDAO.getInstance().add("Added " + listFromDB.get(listLoop).getNewspaperDistributorName() + " to download queue!");

                        // Next newspaper, don't play welcome (only for the most prio one)
                        startPlayingWelcomeForThisNewspaper = false;
                    }
                }
            }
            /*
            // Don't know if we should have this here? Why would we like to play this every time user presses home button and app "restarts"?
            else
            {
                // Play welcome message, and continue playing newspaper after that
                MediaPlayerManager.getInstance().playVoiceMessage(VoiceID.VID_WELCOME_TO_THE_NEWSPAPER.ordinal(), true);
            }
            */

        }

        // Reset the counter that will take you to the menu
        touchCounter = 0;

        // We will also get here after returning from Settings activity, make sure we set back on the Mediaplayer
        MediaPlayerManager.getInstance().setEnabled(true);

        // We will also get here after pressing home button an extra time (first start onNewIntent then, start playing welcome, and then here to this)
        // Don't set the pause to false here
        //MediaPlayerManager.getInstance().setShouldPauseAfterCompletion(false);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sp_shared_preferences),Context.MODE_PRIVATE);
        boolean shutDownApp = sharedPref.getBoolean(getString(R.string.sp_shutdown_application), false); // false = default

        // Set the text on main screen
        TextView infoTextView = (TextView)findViewById(R.id.infoTextView);
        writeTextToMainScreen(infoTextView);

        if (shutDownApp)
            finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            // Unregister Broadcast Receiver
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mHandleMessageReceiverMainActivity);

        } catch (Exception e) {
            LogDAO.getInstance().add(e.getMessage());
        }

        cancelRecurringAlarm();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sp_shared_preferences),Context.MODE_PRIVATE);
        boolean shutDownApp = sharedPref.getBoolean(getString(R.string.sp_shutdown_application), false); // false = default

        if (shutDownApp)
            finish();
    }

    private void setRecurringAlarm()
    {
        Context context = getApplicationContext();

        List<NewspaperDistributor> listFromDB = NewspaperDistributorDAO.getInstance().getAllNewspaperDistributorsSorted();
        for( int listLoop = 0; listLoop < listFromDB.size(); listLoop++)
        {
            NewspaperDistributor currentNewspaperDistributor = listFromDB.get(listLoop);
            if (currentNewspaperDistributor != null && currentNewspaperDistributor.getAutoDownloadEnabled() != 0)
            {
                int dlHour = currentNewspaperDistributor.getDownloadTimeHour();
                int dlMinute = currentNewspaperDistributor.getDownloadTimeMinute();
                long dlInterval = currentNewspaperDistributor.getAutoDownloadInterval();
                String distributorFolderID = currentNewspaperDistributor.getFolderName();

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

                // Put the distributor ID as extra to be able to download it
                Intent downloader = new Intent(context, AlarmReceiver.class);
                downloader.putExtra("NEWSPAPER_DISTRIBUTOR_FOLDER_ID", distributorFolderID);

                // Set the distributor ID as requestCode to the intents broadcast
                // If we don't do this they will have same requestcode (id) and be overwritten by the last one calling the code
                int requestCode = 0;
                try
                {
                    requestCode = Integer.valueOf(distributorFolderID);
                }
                catch (NumberFormatException e)
                {
                    LogDAO.getInstance().add("MainActivity::setRecurringAlarm() - Could not convert id to integer!");
                }

                PendingIntent recurringDownload = PendingIntent.getBroadcast(context, requestCode, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (alarms!=null)
                    alarms.setRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), dlInterval, recurringDownload);
                else
                {
                    LogDAO.getInstance().add("Alarm Manager service is null");
                }

                String text = String.format("Alarm download time set to %02d:%02d", dlHour, dlMinute);
                LogDAO.getInstance().add(text);
            }
        }
    }

    private void cancelRecurringAlarm()
    {
        // Cancel the recurring download if app is closed
        Context ctx = getApplicationContext();

        // Loop through all distributors and cancel all
        List<NewspaperDistributor> listFromDB = NewspaperDistributorDAO.getInstance().getAllNewspaperDistributorsSorted();
        for( int listLoop = 0; listLoop < listFromDB.size(); listLoop++) {
            NewspaperDistributor currentNewspaperDistributor = listFromDB.get(listLoop);
            if (currentNewspaperDistributor != null)
            {
                int requestCode = 0;
                try
                {
                    requestCode = Integer.valueOf(currentNewspaperDistributor.getFolderName());
                }
                catch (NumberFormatException e)
                {
                    LogDAO.getInstance().add("MainActivity::cancelRecurringAlarm() - Could not convert id to integer!");
                }

                Intent downloader = new Intent(ctx, AlarmReceiver.class);
                PendingIntent recurringDownload = PendingIntent.getBroadcast(ctx,requestCode, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                if(alarms != null && recurringDownload != null)
                    alarms.cancel(recurringDownload);
            }
        }
    }
}