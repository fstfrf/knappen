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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

// READ THESE ABOUT ALARMS AND BOOT COMPLETED
// https://developer.android.com/training/scheduling/alarms.html
// https://developer.android.com/reference/android/os/AsyncTask.html

// This class can be called manually from Settings or NewspaperDistributorEdit, and will also be called from DownloaderService during night
public class DownloadFilesAsync extends AsyncTask<String,String,String> {

    private static final String TAG = "DownloadFilesAsync";
    private static boolean mRetryPlayWelcomeAfterDownloading = false;
    private FTPClient mFtp = null;
    private WeakReference<Activity> mActivity;
    private WeakReference<Context> mContext;
    private boolean mStartPlayingWelcomeAfterDownloading;
    private FTPFile[] mServerNewspaperFolderList;            // For example: folder 0 = 20180327, folder 1 = 20180328, folder 2 = 20180329 ...
    private FTPFile[][] mServerNewspaperFileList;            // FTPFile[0][3] ... fourth ftp mp3 file in folder 0
    private NewspaperDistributor mCurrentNewspaperDistributor = null;
    private String mDistributorFolderID = "";                // For example "01"

    // Constructor
    public DownloadFilesAsync(Activity activity, Context context, String distributorFolderID, boolean startPlayingWelcomeAfterDownloading)
    {
        if(activity!= null)
            this.mActivity = new WeakReference<>(activity);    // This will be used for UI updates, will be null if called from service
        this.mContext = new WeakReference<>(context);          // This will be used when this is called from the service
        this.mDistributorFolderID = distributorFolderID;       // This will be used because there can be several downloads going on at the same time
        this.mStartPlayingWelcomeAfterDownloading = startPlayingWelcomeAfterDownloading; // Start playing welcome message after downloading (only first time after reboot)
    }

    protected String doInBackground(String... params) {

        Activity localActivity = null;
        if(mContext == null || mContext.get() == null)
            return "weakref";

        if(mActivity != null && mActivity.get() != null)
            localActivity = mActivity.get();

        // Make sure we have all managers started here if service runs this in background without app open
        // This is a common place where a crash can occur if we expect that the manger is started from application
        LogDAO.getInstance().createDB(mContext.get());
        NewspaperDistributorDAO.getInstance().createDB(mContext.get());
        FileManager.getInstance().setApplicationContext(mContext.get());
        MediaPlayerManager.getInstance().setApplicationContext(mContext.get());
        FTPConnectionManager.getInstance().setApplicationContext(mContext.get());


        // First I thought about looping through all distributors but that is not ok because they are saved separately in distributor list
        // and the admin might have set separate download times for each newspaper distributor.
        if(!mDistributorFolderID.equals("") )
        {
            mCurrentNewspaperDistributor = NewspaperDistributorDAO.getInstance().getNewspaperDistributor(mDistributorFolderID);
        }
        else
        {
            LogDAO.getInstance().add("DownloadFilesAsync error, did not find newspaper distributor!");
            mCurrentNewspaperDistributor = null;
        }

        if (mCurrentNewspaperDistributor == null)
        {
            // Here we could do some more error handling if we want to
            return "fail";
        }

        // Get all distributor data from database
        String newspaperDistributorFolder = mCurrentNewspaperDistributor.getFolderName(); // for example "01"
        String ftpAddress = mCurrentNewspaperDistributor.getFtpAddress();
        int ftpPort = mCurrentNewspaperDistributor.getFtpPort();
        String ftpUsername = mCurrentNewspaperDistributor.getFtpUsername();
        String ftpPassword = mCurrentNewspaperDistributor.getFtpPassword();

        // Stop the mediaplayer while deleting
        MediaPlayerManager.getInstance().stop();
        MediaPlayerManager.getInstance().setEnabled(false);

        // Delete files from old filesystem "/01/Date" ...
        // This function can be removed in next release when we know for sure that everyone has deleted these files
        FileManager.getInstance().deleteAllOldDownloadedFilesAndFolders(localActivity);

        // Delete more than one week old folders
        FileManager.getInstance().checkAndDeleteOldDownloadedFilesAndFolders(newspaperDistributorFolder);

        // Enable mediaplayer again
        MediaPlayerManager.getInstance().setEnabled(true);

        // Set the filemanager to start from first file (in the newest local folder set above)
        FileManager.getInstance().resetCurrentIndex();

        // Also set the media player to start from beginning of file (if we have paused in the middle of some other file)
        MediaPlayerManager.getInstance().resetCurrentPosition();

        // Create the local list
        LogDAO.getInstance().add("Updating the local folder list");
        FileManager.getInstance().updateLocalFileLists(newspaperDistributorFolder); // Getting ÖT list and saved in local list in FileManager (used in compareServerAndLocalList function)
        FileManager.getInstance().updateUIThread(localActivity);

        // Test if we are online
        boolean connected = FTPConnectionManager.getInstance().isOnline();
        int counter = 0;
        while (!connected && counter < 5)
        {
            SystemClock.sleep(2000); // Sleep 2 seconds and test again
            connected = FTPConnectionManager.getInstance().isOnline();
            counter++;
        }

        // Just a last test, abort if not functioning after 10 seconds
        connected = FTPConnectionManager.getInstance().isOnline();
        if(!connected)
        {
            LogDAO.getInstance().add("Application could not connect to internet.");
            System.err.println("Application could not connect to internet.");
            return "fail";
        }

        // Connect to FTP
        mFtp = FTPConnectionManager.getInstance().connectToFTP(ftpAddress, ftpPort, ftpUsername, ftpPassword);
        FileManager.getInstance().updateUIThread(localActivity);

        // Create the FTP server list
        LogDAO.getInstance().add("Creating ftp server list");
        createServerList();
        FileManager.getInstance().updateUIThread(localActivity);

        // Compare and make download list
        LogDAO.getInstance().add("Comparing files for " + mCurrentNewspaperDistributor.getNewspaperDistributorName() + " (" + mCurrentNewspaperDistributor.getFolderName() + ")");
        FTPFile[][] downloadList = FileManager.getInstance().compareServerAndLocalList(mServerNewspaperFolderList, mServerNewspaperFileList);
        FileManager.getInstance().updateUIThread(localActivity);

        // Here we could check available space

        // Download missing folders and files from downloadList
        LogDAO.getInstance().add("Starting to download...");

        // Extra check for the case if you download a newspaper that is often empty (for example KP)
        // On a restart of phone only the first one will get the boolean set to true (startPlayingWelcome), but in this case it will jump out because the download list is null
        // that also means the updateToNewestPlayableFolder() won't be run until after the next newspaper has been fully downloaded
        if (mRetryPlayWelcomeAfterDownloading) {
            mStartPlayingWelcomeAfterDownloading = true;
            mRetryPlayWelcomeAfterDownloading = false;
        }
        // Try to download files, returns false if download list is null
        boolean didDownloadFiles = downloadAndSaveFiles(downloadList, mStartPlayingWelcomeAfterDownloading);

        // This will make the next newspaper play welcome (instead of only the first, if the first is empty)
        if (!didDownloadFiles && mStartPlayingWelcomeAfterDownloading)
            mRetryPlayWelcomeAfterDownloading = true;

        FileManager.getInstance().updateUIThread(localActivity);

        // Disconnect when we are done
        FTPConnectionManager.getInstance().disconnectFromFTP(mFtp);

        // Make sure we reset this
        mFtp = null;

        // Make sure to run the local update for playable folder, it will set the newest local folder to be able to play it in the morning!
        FileManager.getInstance().updateToNewestPlayableFolder();
        FileManager.getInstance().gotoFirstPlayableNewspaperInCurrentDate();

        // The return value here will go into onPostExecute as result
        return "ok";
    }

    protected void onProgressUpdate(String... progress) {
        //setProgressPercent(progress[0]);
    }

    // This function is not in async anymore, that means we can do UI stuff here (for example showing Toast)
    protected void onPostExecute(String result) {

        // Ok, we'll get here when done downloading
        Activity localActivity = null;
        if (mActivity != null && mActivity.get() != null)
            localActivity = mActivity.get();

        // Send Broadcast to Broadcast receiver with message
        FileManager.getInstance().updateUIThread(localActivity);
    }

    // Create a list of the folder and files on the ftp server
    private void createServerList()
    {
        int fileCounter = 0;

        Activity localActivity = null;
        if (mActivity != null && mActivity.get() != null)
            localActivity = mActivity.get();

        if (mFtp == null)
        {
            LogDAO.getInstance().add("createServerList() mFTP = null");
            return;
        }

        try
        {
            // Get the folder list from ftp (for example 20180328, 20180329 etc)
            mServerNewspaperFolderList = mFtp.listDirectories();

            if(mServerNewspaperFolderList != null)
            {
                // Allocate space for the folder id
                mServerNewspaperFileList = new FTPFile[mServerNewspaperFolderList.length][];

                // Loop through all newspaper folders on server
                for(int folderID = 0; folderID < mServerNewspaperFolderList.length; folderID++)
                {
                    // Make sure it is a folder
                    if( !mServerNewspaperFolderList[folderID].isDirectory() )
                        break;

                    //Log.d(TAG,"Dir: " + folderID + " - " + mServerNewspaperFolderList[folderID].getName());

                    String folderName = mServerNewspaperFolderList[folderID].getName();

                    // Change directory on the ftp server
                    if (mFtp.changeWorkingDirectory("/" + folderName))
                    {
                        LogDAO.getInstance().add("Parsing files in ftp folder: " + folderName);
                        FileManager.getInstance().updateUIThread(localActivity);

                        // Get all files in the current ftp directory into array
                        FTPFile[] files = mFtp.listFiles(); // No sorting needed here, the check will be done with folder names in FileManager::compareServerAndLocalList()
                        if (files != null)
                        {
                            // Allocate space for the files id
                            // If the folder is empty, the mServerNewspaperFileList[xx] = null
                            // Note, this will allocate space even for files that are not mp3 files
                            mServerNewspaperFileList[folderID] = new FTPFile[files.length];
                            fileCounter = 0;

                            // Loop through all files in current folder
                            for(int fileID = 0; fileID < files.length; fileID++)
                            {
                                // Only copy .mp3 files
                                if (files[fileID].isFile() && files[fileID].getName().startsWith("speechgen") && files[fileID].getName().endsWith(".mp3"))
                                {
                                    // Save the files to our own server list array
                                    mServerNewspaperFileList[folderID][fileCounter] = files[fileID];
                                    fileCounter++;

                                    //Log.d(TAG, "File: " + fileID + " - " + files[fileID].getName());
                                }
                            }
                        }
                    }
                    else
                        LogDAO.getInstance().add("Working directory changed failed. Reply: " + mFtp.getReplyString());
                }
            }
        }
        catch (IOException e)
        {
            Log.d(TAG, e.toString());
            LogDAO.getInstance().add("IOException: " + e.toString());
        }
    }

    private boolean downloadAndSaveFiles(FTPFile[][] downloadList, boolean playWelcomeAfterFirstDownloadedFile)
    {
        LogDAO.getInstance().add( "Downloading files from ftp");
        int totalFilesDownloaded = 0;

        Activity localActivity = null;
        if (mActivity != null && mActivity.get() != null)
            localActivity = mActivity.get();

        if (mFtp == null)
        {
            LogDAO.getInstance().add("downloadAndSaveFiles() mFTP = null");
            return false;
        }

        try {

            // First check to see if we should exit download
            if (downloadList == null) {
                LogDAO.getInstance().add("No files to download!");
                return false;
            }

            // Loop through downloadList and download all folders and files listed
            // Loop backwards, meaning we will always start with the newest
            for( int folderID = downloadList.length -1; folderID >= 0; folderID--)
            {
                if (downloadList[folderID] != null)
                {
                    // Before we start downloading, make sure we are in the correct directory on the ftp
                    String folderName = mServerNewspaperFolderList[folderID].getName();

                    if (!mFtp.changeWorkingDirectory("/" + folderName))
                        LogDAO.getInstance().add("Working directory changed failed. Reply: " + mFtp.getReplyString());

                    if(mContext.get() == null)
                        continue;

                    // We also need to create the folder locally
                    String fullFolderPath = mContext.get().getFilesDir() + "/" + folderName + "/" + mDistributorFolderID;
                    File localFolder = new File (fullFolderPath);

                    // Check if we already have created the directory
                    if (localFolder.exists() && localFolder.isDirectory())
                    {
                        // It should never come here as the download code works now
                    }
                    else
                    {
                        // Create the folder locally
                        if (!localFolder.mkdirs())
                        {
                            LogDAO.getInstance().add("Failed to create local folder.");
                            return false;
                        }
                    }

                    // Ok, now we have created the local folder on phone
                    // Loop through list and download each file from our downloadList array
                    for( int fileID = 0; fileID < downloadList[folderID].length; fileID++)
                    {
                        if (downloadList[folderID][fileID] != null)
                        {
                            String fileName = downloadList[folderID][fileID].getName(); // speechgen0001.mp3 etc

                            // Create a local file and download (retrieve) the files data
                            File localFile = new File(fullFolderPath, fileName);
                            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(localFile));
                            boolean success = mFtp.retrieveFile(fileName, outputStream);
                            if (success)
                            {
                                // Write out the downloaded file to log, only if activity is set, which means you are actually watching the log list in Settings activity
                                if (localActivity != null)
                                    LogDAO.getInstance().add("Downloaded /" + folderName + "/" + mDistributorFolderID + "/" + fileName);
                                FileManager.getInstance().updateUIThread(localActivity);
                                totalFilesDownloaded++;
                            }
                            else
                            {
                                // Log error
                                LogDAO.getInstance().add("retrieveFile() failed: " + fileName);
                            }

                            // Always close the stream
                            outputStream.close();
                            outputStream = null; // GC
                            localFile = null; // GC

                            // Start playing welcome message after first file has been downloaded
                            if (playWelcomeAfterFirstDownloadedFile)
                            {
                                // Update the local folder list, this will set the newest newspaper folder to be set for playing
                                FileManager.getInstance().updateToNewestPlayableFolder();
                                FileManager.getInstance().gotoFirstPlayableNewspaperInCurrentDate();

                                // Ok, time to start the welcome message
                                sendPlayWelcome();

                                // Reset the boolean, we don't want to play welcome again, especially not after starting to download first file from next folder
                                playWelcomeAfterFirstDownloadedFile = false;
                            }
                        }
                    }
                }
            }

            // Ok, now we are done downloading all files
            LogDAO.getInstance().add("Number of files downloaded: " + String.valueOf(totalFilesDownloaded));

            // Start playing welcome message even if we notice that there are no new folder/files, but we have still rebooted phone
            if (playWelcomeAfterFirstDownloadedFile && totalFilesDownloaded == 0)
            {
                // Update the local folder list, this will set the newest newspaper folder to be set for playing
                FileManager.getInstance().updateToNewestPlayableFolder();
                FileManager.getInstance().gotoFirstPlayableNewspaperInCurrentDate();

                // Ok, time to start the welcome message
                sendPlayWelcome();
            }

            return true;
        }
        catch (IOException e)
        {
            LogDAO.getInstance().add( "Downloading error: " + e.toString());
            return false;
        }
    }

    // THIS FUNCTION SHOULD BE RUN FROM ASYNC THREAD
    public void sendPlayWelcome() {
        // This function sends a broadcast to the UI thread and plays the welcome message
        // You need to do it this way because it might not be a good idea to run the play from async
        if(mContext == null || mContext.get() == null)
            return;

        Intent intent = new Intent(mContext.get().getString(R.string.broadcastToMainActivity));
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES); //If set, this intent will always match any components in packages that are currently stopped.
        intent.putExtra("FSTF_PlayWelcome_KEY", true);
        LocalBroadcastManager.getInstance(mContext.get()).sendBroadcast(intent);
    }
}
