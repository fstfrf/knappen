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
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;

import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

// Make these available everywhere
enum VoiceID {
    VID_WELCOME_TO_THE_NEWSPAPER,
    VID_ONE_MOMENT_NEWSPAPER_LOADING,
    //VID_UPDATING,
    VID_CONTINUING,
    VID_PAUSING,
    VID_NEXT_ARTICLE,
    VID_PREVIOUS_ARTICLE,
    VID_NEXT_NEWSPAPER,
    VID_PREVIOUS_NEWSPAPER,
    //VID_NEXT_DISTRIBUTOR,
    //VID_PREVIOUS_DISTRIBUTOR,
    //VID_YOU_HAVE_REACHED_THE_END_OF_THE_DISTRIBUTOR,
    VID_YOU_HAVE_REACHED_THE_END_OF_THE_NEWSPAPER,
    //VID_INCREASING_SPEED,
    //VID_DECREASING_SPEED,
    //VID_NORMAL_SPEED,
    //VID_YOU_HAVE_REACHED_MAX_SPEED,

    VID_MAX_NUM_ENUMS
}

// This is the FileManager class that will handle all local files (mp3) used by the app
public class FileManager {

    private WeakReference<Context> context = null;              // Save the context to be used in functions
    private static Uri voices[];                                // uri of all the voices used in app
    private String preFileName = "speechgen";                   // prefix
    private String fileFormat = "mp3";                          // postfix
    private int currentIndex = 1;                               // The 1 in speechgen0001.mp3
    private String currentNewspaperFolderDateForPlayback = "";  // The current newspaper folder used (for example 20190521)
    private String currentDistributorFolderForPlayback = "";    // The current distributor folder used for playing files
    private File[] localNewspaperDateFolderList;                // For example 20180327, 20180328, 20180329
    private File[][] localNewspaperFileList;                    // localNewspaperFileList[0][0] = first file in first folder

    // Make it a static class (only one instance) | same as singleton
    private static FileManager instance = new FileManager();
    public static synchronized FileManager getInstance() {
        return instance;
    }

    // This must be set before accessing context
    public void setApplicationContext(Context cx)
    {
        context = new WeakReference<>(cx);

        if(context.get() != null && voices == null)
        {
            // Create an array with uri:s
            voices = new Uri[VoiceID.VID_MAX_NUM_ENUMS.ordinal()];

            // And create these only once
            voices[VoiceID.VID_WELCOME_TO_THE_NEWSPAPER.ordinal()] = Uri.parse("android.resource://" + context.get().getPackageName() + "/" + R.raw.p001);
            voices[VoiceID.VID_ONE_MOMENT_NEWSPAPER_LOADING.ordinal()] = Uri.parse("android.resource://" + context.get().getPackageName() + "/" + R.raw.p002);
            voices[VoiceID.VID_CONTINUING.ordinal()] = Uri.parse("android.resource://" + context.get().getPackageName() + "/" + R.raw.p004);
            voices[VoiceID.VID_PAUSING.ordinal()] = Uri.parse("android.resource://" + context.get().getPackageName() + "/" + R.raw.p005);
            voices[VoiceID.VID_NEXT_ARTICLE.ordinal()] = Uri.parse("android.resource://" + context.get().getPackageName() + "/" + R.raw.p006);
            voices[VoiceID.VID_PREVIOUS_ARTICLE.ordinal()] = Uri.parse("android.resource://" + context.get().getPackageName() + "/" + R.raw.p007);
            voices[VoiceID.VID_NEXT_NEWSPAPER.ordinal()] = Uri.parse("android.resource://" + context.get().getPackageName() + "/" + R.raw.p008);
            voices[VoiceID.VID_PREVIOUS_NEWSPAPER.ordinal()] = Uri.parse("android.resource://" + context.get().getPackageName() + "/" + R.raw.p009);
            voices[VoiceID.VID_YOU_HAVE_REACHED_THE_END_OF_THE_NEWSPAPER.ordinal()] = Uri.parse("android.resource://" + context.get().getPackageName() + "/" + R.raw.p013);
        }
    }

    // START currentIndex - Use these synchronized functions for current index!
    public synchronized void resetCurrentIndex()
    {
        // Reset the current index to start from 1 (speechgen0001.mp3)
        currentIndex = 1;
    }
    public synchronized int getCurrentIndex()
    {
        return currentIndex;
    }
    public synchronized void increaseCurrentIndex()
    {
        currentIndex++;
    }
    public synchronized void decreaseCurrentIndex()
    {
        currentIndex--;
    }
    // END currentIndex


    public void updateLocalFileLists(String newspaperDistributorID)
    {
        if (context == null || context.get() == null) {
            LogDAO.getInstance().add("FileManager::updateLocalFileLists() - context is null");
            return;
        }

        // Get the list, this can be unsorted even if folder names are sorted
        File newspaperDateFolder = context.get().getFilesDir(); // Base directory

        // Check if it actually exist ... and is a directory
        if (newspaperDateFolder.exists() && newspaperDateFolder.isDirectory())
        {
            // List all folders with date
            localNewspaperDateFolderList = null; // GC
            localNewspaperDateFolderList = newspaperDateFolder.listFiles(); // No sorting needed here // 20190521, 20190522, 20190523

            localNewspaperFileList = null; // GC

            // Allocate memory for the array
            if( localNewspaperDateFolderList.length > 0 )
            {
                localNewspaperFileList = new File[localNewspaperDateFolderList.length][];
            }

            // Loop through local date folders and setup the file lists
            for(int localFolderID = 0; localFolderID < localNewspaperDateFolderList.length; localFolderID++)
            {
                // Check if our distributor exist inside this folder
                String newspaperDistributorFolderPath = localNewspaperDateFolderList[localFolderID] + "/" + newspaperDistributorID;
                File newspaperDistributorFolder = new File (newspaperDistributorFolderPath);

                // Check if it actually exist ... and is a directory
                if (newspaperDistributorFolder.exists() && newspaperDistributorFolder.isDirectory())
                {
                    // Yes, we now know that we have a "01 folder for example

                    // List all files in the folder /20190521/01/...
                    localNewspaperFileList[localFolderID] = newspaperDistributorFolder.listFiles(); // Sorting below

                    // We need to sort the array, Android might give you a sorted list ... or NOT!
                    Arrays.sort(localNewspaperFileList[localFolderID]);
                }
            }
        }
    }

    // This function will update to latest (newest) folder, return true on success
    public boolean updateToNewestPlayableFolder() {
        if (context == null || context.get() == null)
            return false;

        int newestLocalFolderID = 0;

        File baseFolder = context.get().getFilesDir();

        // Check if it actually exist ... and is a directory
        if (baseFolder.exists() && baseFolder.isDirectory()) {
            // List all files in the base folder
            File[] localNewspaperDateFolderList = baseFolder.listFiles(); // No sorting needed here

            // Loop through local app folders to see which one that is the newest
            // One possible way would be to pick the last folder but we don't know for sure that this will give a sorted folder list
            for (int loopFolderID = 0; loopFolderID < localNewspaperDateFolderList.length; loopFolderID++) {
                if (localNewspaperDateFolderList[loopFolderID].getName().compareTo(localNewspaperDateFolderList[newestLocalFolderID].getName()) > 0) {
                    // 0 = equal
                    // < = localFolderID is less than newestLocalFolderID
                    // > = localFolderID is greater than newestLocalFolderID

                    // ok, we found a newer folder
                    newestLocalFolderID = loopFolderID;
                }
            }

            // Ok, now we know which folder that is newest (if it's not empty)
            if (localNewspaperDateFolderList.length > 0) {
                String dateFolderName = localNewspaperDateFolderList[newestLocalFolderID].getName();

                // Set the current to the newest folder
                setCurrentNewspaperFolderDateForPlayback(dateFolderName);
                return true;
            }
        }
        return false;
    }

    public FTPFile[][] compareServerAndLocalList(FTPFile[] serverNewspaperFolderList, FTPFile[][] serverNewspaperFileList)
    {
        //int newestLocalFolderID = 0;

        if (serverNewspaperFolderList != null && serverNewspaperFolderList.length > 0)
        {
            // First, allocate as many newspapers as we have on server totally (folders)
            // The download list will contain all folders and files that should be downloaded, can be null!
            FTPFile[][] downloadList = new FTPFile[serverNewspaperFolderList.length][];

            // Loop through the server folder list
            // Start from the end, if the server folders are sorted we can opt out to optimize this later
            // Download all folders that are newer than the newest local folder
            for(int serverFolderID = serverNewspaperFolderList.length -1; serverFolderID >= 0; serverFolderID--)
            {
                // Local folder list empty (first time before any date folders are created or everything deleted)
                if (localNewspaperDateFolderList == null || localNewspaperDateFolderList.length == 0)
                {
                    downloadList[serverFolderID] = new FTPFile[serverNewspaperFileList[serverFolderID].length];
                    downloadList[serverFolderID] = serverNewspaperFileList[serverFolderID];
                    continue;
                }

                boolean foundServerFolder = false;

                // Loop through the local folder list for each server folder and compare
                for(int localFolderLoopID = 0; localFolderLoopID < localNewspaperDateFolderList.length; localFolderLoopID++)
                {
                    String localFolderName = localNewspaperDateFolderList[localFolderLoopID].getName(); // 20190521 for example

                    if (serverNewspaperFolderList[serverFolderID].getName().compareTo(localFolderName) == 0 )
                    {
                        // == 0 -> equal
                        foundServerFolder = true;

                        // Allocate memory before we loop through the list
                        downloadList[serverFolderID] = new FTPFile[serverNewspaperFileList[serverFolderID].length];

                        // Ok, we have the folder on both server ftp and locally
                        // Now we need to loop through all files, because the phone may have been restarted in the middle of download process and all files are not guaranteed to exist
                        // Note to programmers, if you would delete a file manually in the middle of mp3 range (for example speechgen0034.mp3) on server or locally, that file would be missing
                        // and would mess up the local list or server list to be out of sync -> 34 locally would be compared to filesize of file 35 on server etc.. which means it will download
                        // all files (even if 35 would match 35 with the file size, they would have different array ID).
                        // However, this works great as it is now and will sync local folders to be identical as server.
                        int serverFilesNum = serverNewspaperFileList[serverFolderID].length;
                        for(int fileID = 0; fileID < serverFilesNum; fileID++)
                        {
                            long serverFileSize = 0;
                            long localFileSize = 0;

                            // Get the file size on server
                            if(serverNewspaperFileList[serverFolderID][fileID] != null)
                                serverFileSize = serverNewspaperFileList[serverFolderID][fileID].getSize();

                            if(localNewspaperFileList != null && localNewspaperFileList[localFolderLoopID] != null && fileID < localNewspaperFileList[localFolderLoopID].length && localNewspaperFileList[localFolderLoopID][fileID] != null)
                            {
                                // Get the local file size
                                localFileSize = localNewspaperFileList[localFolderLoopID][fileID].length();
                            }

                            if (localFileSize != serverFileSize)
                            {

                                // The localNewspaperFileList[localFolderID][fileID].length() will not be 100% same every time we check the list
                                long diff = serverFileSize - localFileSize;
                                long absDiff = 0;
                                try
                                {
                                    absDiff = Math.abs(diff);
                                } catch (Exception e)
                                {
                                    absDiff = diff;
                                }

                                if (localFileSize != 0) // We don't need to show this for files that does not exist locally
                                    LogDAO.getInstance().add("/" + localFolderName + "/" + serverNewspaperFileList[serverFolderID][fileID].getName() + " differs with bytes: " + Long.toString(absDiff));

                                if (!BuildConfig.DEBUG || (BuildConfig.DEBUG && absDiff > 32)) // take alignment into account (only differs in debug version)
                                {
                                    // Mark the file for download
                                    downloadList[serverFolderID][fileID] = serverNewspaperFileList[serverFolderID][fileID];
                                }
                            }
                            else
                            {
                                // Don't download, we already have the same file locally
                                downloadList[serverFolderID][fileID] = null;
                            }
                        } // end for loop for files

                        continue; // We can skip the rest of local folders, will be looped through again on next server folder
                    }
                }

                if(!foundServerFolder)
                {
                    // Ok, when we get here we know that the folder on server does not at all exist locally, download whole folder!

                    // NOTE, the serverNewspaperFileList[serverFolderID].length can be about double size (because of calculated non mp3 files in the folder)
                    downloadList[serverFolderID] = new FTPFile[serverNewspaperFileList[serverFolderID].length];
                    downloadList[serverFolderID] = serverNewspaperFileList[serverFolderID];
                }

            }
            return  downloadList;
        }
        return null;
    }

    public void setCurrentNewspaperFolderDateForPlayback(String folderName)
    {
        currentNewspaperFolderDateForPlayback = folderName;
    }

    private void setDistributorFolderForPlayback(String distributorFolder)
    {
        currentDistributorFolderForPlayback = distributorFolder;
    }

    public String getCurrentNewspaperFolderDateForPlayback()
    {
        return currentNewspaperFolderDateForPlayback;
    }

    public boolean gotoNextPlayableFile()
    {
        // Increase index
        increaseCurrentIndex();

        // Check if the file exist (with the new index)
        if (!checkIfCurrentFileExist())
        {
            // Reset the index, BUT stop playing files! This behaviour was requested.
            resetCurrentIndex();

            return false;
        }
        return true;
    }

    public boolean gotoPreviousPlayableFile()
    {
        // Decrease index
        decreaseCurrentIndex();

        // Check if the file exist (with the new index)
        if (!checkIfCurrentFileExist())
        {
            // Reset the index, BUT stop playing files! This behaviour was requested.
            resetCurrentIndex();

            return false;
        }
        return true;
    }
/*
    // Save this code, worked for one distributor, needs to be updated to handle several
    public boolean gotoNextPlayableFolder()
    {
        // Always reset the current index for the file to be played = speechgen0001.mp3
        resetCurrentIndex();

        if(context == null || context.get() == null)
            return false;

        // Check if newsPaperDistributorID folder exist, if not - exit
        String newspaperDistributorFolderPath = context.get().getFilesDir() + "/" + currentDistributorFolderForPlayback;
        File newspaperDistributorFolder = new File(newspaperDistributorFolderPath);
        if (!newspaperDistributorFolder.exists()) {
            return false;
        }

        // Create a list of all files in folder, add them to a sorted list, sort them and get next folder name!
        String[] stringFileNameList =  newspaperDistributorFolder.list();
        if (stringFileNameList!=null && stringFileNameList.length > 0)
        {
            List<String> sortedFileNameList = new ArrayList<String>();
            for(int i=0; i < stringFileNameList.length; i++)
            {
                sortedFileNameList.add(stringFileNameList[i]);
            }

            Collections.sort(sortedFileNameList, new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    return s1.compareTo(s2);
                }
            });


            // The list is now guaranteed to be sorted
            // Get the current folder ID from "currentFolder"
            int currentFolderListID = 0;
            for(int i=0; i<sortedFileNameList.size(); i++) {
                if (sortedFileNameList.get(i).compareTo(currentNewspaperFolderDateForPlayback) == 0) {
                    // Found current folder!!
                    currentFolderListID = i;
                }
            }

            // Check if next folder is available
            String nextFolderName = "";
            currentFolderListID++;

            if(currentFolderListID < sortedFileNameList.size())
            {
                // Get next folder
                nextFolderName = sortedFileNameList.get(currentFolderListID);
            }
            else
            {
                // Go back to first folder
                nextFolderName = sortedFileNameList.get(0);
            }

            String fullFolderPath = context.get().getFilesDir() + "/" + currentDistributorFolderForPlayback + "/" + nextFolderName;

            // Check if first file is in the list
            File dir = new File (fullFolderPath);
            if (dir.exists())
            {
                // Yes, we have found a new folder that exists
                setCurrentNewspaperFolderDateForPlayback(nextFolderName);

                if (checkIfCurrentFileExist())
                {
                    // YES, we found a folder with a new file speechgen0001.mp3
                    return true;
                }
            }
        }

        return false;
    }*/

    public boolean gotoPreviousPlayableFolder()
    {
        // Always reset the current index for the file to be played = speechgen0001.mp3
        resetCurrentIndex();

        if(context == null || context.get() == null)
            return false;

        // First, check if we have other distributors inside the same date folder
        List<NewspaperDistributor> distributorList = NewspaperDistributorDAO.getInstance().getAllNewspaperDistributorsSorted();
        boolean foundCurrentDistributor = false;
        for( int listLoop = 0; listLoop < distributorList.size(); listLoop++) {
            NewspaperDistributor loopNewspaperDistributor = distributorList.get(listLoop);
            if (loopNewspaperDistributor != null)
            {
                if (foundCurrentDistributor)
                {
                    // This means that we have found the current distributor before in this loop
                    // And check if this distributor exist after in the list

                    // Check if folder exist inside our current date folder
                    String nextFolderPath = context.get().getFilesDir() + "/" + currentNewspaperFolderDateForPlayback + "/" + loopNewspaperDistributor.getFolderName();

                    // Check if first file is in the list
                    File dir = new File (nextFolderPath);
                    if (dir.exists())
                    {
                        setDistributorFolderForPlayback(loopNewspaperDistributor.getFolderName());

                        if (checkIfCurrentFileExist())
                        {
                            // YES, we found a folder with a new file speechgen0001.mp3
                            return true;
                        }
                    }
                }

                if (loopNewspaperDistributor.getFolderName().compareTo(currentDistributorFolderForPlayback) == 0) {
                    foundCurrentDistributor = true;
                }
            }
        }

        // If we get here we know that there were no other distributors in the same date folder

        // Create a list of all files in base folder, add them to a sorted list, sort them and get next date folder name!
        File baseFolder = context.get().getFilesDir();
        String[] stringDateFileNameList = baseFolder.list();
        if (stringDateFileNameList!=null && stringDateFileNameList.length > 0)
        {
            List<String> sortedDateFileNameList = new ArrayList<String>();
            for(int i=0; i < stringDateFileNameList.length; i++)
            {
                sortedDateFileNameList.add(stringDateFileNameList[i]);
            }

            Collections.sort(sortedDateFileNameList, new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    return s1.compareTo(s2);
                }
            });


            // The list is now guaranteed to be sorted (oldest first, newest last)
            // Get the current ID from date folder list
            int currentFolderListID = 0;
            for(int i=0; i<sortedDateFileNameList.size(); i++) {
                if (sortedDateFileNameList.get(i).compareTo(currentNewspaperFolderDateForPlayback) == 0) {
                    // Found current folder!!
                    currentFolderListID = i;
                    break;
                }
            }

            // Check if next folder is available
            String nextFolderDateName = "";
            currentFolderListID--;

            if(currentFolderListID >= 0) // If value is positive
            {
                // Get previous folder
                nextFolderDateName = sortedDateFileNameList.get(currentFolderListID);
            }
            else
            {
                // Go back to newest folder
                nextFolderDateName = sortedDateFileNameList.get(sortedDateFileNameList.size()-1);
            }

            // We now know which date folder to start playing from
            // BUT, now we need to reset the distributor, we can't use currentDistributorFolderForPlayback because there might be a more prioritized distributor in this folder

            // Loop through sorted/prioritized distributor the list again
            for( int listLoop = 0; listLoop < distributorList.size(); listLoop++) {

                NewspaperDistributor loopNewspaperDistributorInNewDateFolder = distributorList.get(listLoop);
                if (loopNewspaperDistributorInNewDateFolder != null) {

                    // Try with this one in list
                    String fullFolderPath = context.get().getFilesDir() + "/" + nextFolderDateName + "/" + loopNewspaperDistributorInNewDateFolder.getFolderName();

                    // Check if first file is in the list
                    File dir = new File (fullFolderPath);
                    if (dir.exists())
                    {
                        // Yes, we have found a new folder that exists
                        setCurrentNewspaperFolderDateForPlayback(nextFolderDateName);
                        setDistributorFolderForPlayback(loopNewspaperDistributorInNewDateFolder.getFolderName());

                        if (checkIfCurrentFileExist())
                        {
                            // YES, we found a folder with a new file speechgen0001.mp3
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean gotoFirstPlayableNewspaperInCurrentDate()
    {
        // Always reset
        resetCurrentIndex();

        if (context == null || context.get() == null)
            return false;

        String currentDateSet = getCurrentNewspaperFolderDateForPlayback();

        // First, check if we have other distributors inside the same date folder
        List<NewspaperDistributor> distributorList = NewspaperDistributorDAO.getInstance().getAllNewspaperDistributorsSorted();
        for( int listLoop = 0; listLoop < distributorList.size(); listLoop++) {
            NewspaperDistributor loopNewspaperDistributor = distributorList.get(listLoop);
            if (loopNewspaperDistributor != null)
            {
                String folderPath = context.get().getFilesDir() + "/" + currentDateSet + "/" + loopNewspaperDistributor.getFolderName();
                File newspaperDistributorFolder = new File(folderPath);
                if (newspaperDistributorFolder != null && newspaperDistributorFolder.exists())
                {
                    setDistributorFolderForPlayback(loopNewspaperDistributor.getFolderName());
                    return true;
                }
            }
        }

        // No newspapers in this date folder at all
        return false;
    }

    public boolean checkIfCurrentFileExist()
    {
        if (context == null || context.get() == null)
            return false;

        // When going backwards, the current index can become negative
        if(getCurrentIndex() < 0)
            return false;

        // Get a string in format "speechgen0001.mp3"
        String currentFileName = String.format("%s%04d.%s", preFileName, getCurrentIndex(), fileFormat); // here we have 9999 as max number of articles on one paper
        String fullPath = context.get().getFilesDir() + "/" + currentNewspaperFolderDateForPlayback + "/" + currentDistributorFolderForPlayback + "/" + currentFileName;

        if (fileExist(fullPath))
            return true;
        else
            return false;
    }

    public Uri getCurrentPlayableMediaFileUri()
    {
        if (context == null || context.get() == null)
            return null;

        // EXTRA
        //StringUtils.leftPad
        //String formatting = String.format("%%s%%0%dd.%%s", numIndexNumbersAfterFile); // This gives us possibility to extend to more than 9999 articles in one paper
        //String currentFileName = String.format(formatting, preFileName, getCurrentIndex(), fileFormat);

        // Get a string in format "speechgen0001.mp3"
        String currentFileName = String.format("%s%04d.%s", preFileName, getCurrentIndex(), fileFormat); // here we have 9999 as max number of articles on one paper

        String fullPath = context.get().getFilesDir() + "/" + currentNewspaperFolderDateForPlayback + "/" + currentDistributorFolderForPlayback + "/" + currentFileName;

        if (fileExist(fullPath))
            return Uri.parse(fullPath);
        else
            return null;
    }

    public Uri getVoiceUri(int voiceID)
    {
        if(voices != null && voiceID >=0 && voiceID < VoiceID.VID_MAX_NUM_ENUMS.ordinal())
        {
            return voices[voiceID];
        }
        return null;
    }

    public void deleteAllOldDownloadedFilesAndFolders(Activity activity)
    {
        if (context == null || context.get() == null)
            return;

        // Check if newsPaperDistributorID folder exist, if not - exit
        String newspaperDistributorFolderPath = context.get().getFilesDir() + "/01"; // Old file system was "01" for first newspaper
        File newspaperDistributorFolder = new File (newspaperDistributorFolderPath);
        if (!newspaperDistributorFolder.exists()) {
            return;
        }

        LogDAO.getInstance().add("Deleting old files. Please wait until finished!");
        int filesDeleted = 0;

        // List all files in the newspaperDistributorFolder, for example "ÖT"
        File[] localFolderList = newspaperDistributorFolder.listFiles(); // No sorting needed here
        if(localFolderList != null)
        {
            // Check all newspaper folders (for example 180327 ... )
            for(File currentNewspaperFolder : localFolderList)
            {
                String newspaperFolderName = currentNewspaperFolder.getName();
                String fullFolderPath = context.get().getFilesDir() + "/01/" + newspaperFolderName;

                // Get the files from directory
                File dir = new File (fullFolderPath);
                File[] filesInDir = dir.listFiles(); // No sorting needed here
                if (filesInDir != null)
                {
                    // Loop through all files
                    for(File currentFile : filesInDir)
                    {
                        // Delete the mp3 file in the folder
                        deleteFileOrFolder(currentFile, "/01/" + newspaperFolderName + "/" + currentFile.getName());
                        //updateUIThread(activity);
                        filesDeleted++;
                    }
                }

                // The folder must be empty to delete it, that's why we needed to loop through all files and delete them
                // This is the folder with date, for example "20180328"
                deleteFileOrFolder(dir, "/01/" + newspaperFolderName);
                updateUIThread(activity);
            }
        }

        // Finally delete the distribution folder ("01" for ÖT for example)
        deleteFileOrFolder(newspaperDistributorFolder, "/01");

        String text = "Deleted " + String.valueOf(filesDeleted) + " old files";
        LogDAO.getInstance().add(text);

        // Refresh log
        updateUIThread(activity);
    }

    public void deleteAllDownloadedFilesAndFolders(Activity activity, String newsPaperDistributorID)
    {
        if (context == null || context.get() == null)
            return;

        int filesDeleted = 0;

        // Get the list, this can be unsorted even if folder names are sorted
        File newspaperDateFolder = context.get().getFilesDir(); // Base directory

        // Check if it actually exist ... and is a directory
        if (newspaperDateFolder.exists() && newspaperDateFolder.isDirectory()) {

            // List all folders with date
            File[] localFolderList = newspaperDateFolder.listFiles(); // No sorting needed here, 20190521, 20190522, 20190523
            if (localFolderList != null) {

                LogDAO.getInstance().add("Deleting files. Please wait until finished!");

                // Check all newspaper folders (for example 180327)
                for(File currentNewspaperFolder : localFolderList)
                {
                    String newspaperFolderName = currentNewspaperFolder.getName();
                    String fullFolderPath = context.get().getFilesDir() + "/" + newspaperFolderName + "/" + newsPaperDistributorID;

                    // Get the files from directory
                    File dir = new File (fullFolderPath);

                    // Extra check because the newspaper distributor doesn't have to exist here with new folder system
                    if (dir.exists())
                    {
                        File[] filesInDir = dir.listFiles(); // No sorting needed here
                        if (filesInDir != null)
                        {
                            // Loop through all files
                            for(File currentFile : filesInDir)
                            {
                                // Delete the mp3 file in the folder
                                deleteFileOrFolder(currentFile, "/" + newspaperFolderName + "/" + newsPaperDistributorID + "/" + currentFile.getName());
                                //updateUIThread(activity);
                                filesDeleted++;
                            }
                        }

                        // The folder must be empty to delete it, that's why we needed to loop through all files and delete them
                        // This is the folder with date, for example "20180328"
                        deleteFileOrFolder(dir, "/" + newspaperFolderName + "/" + newsPaperDistributorID);
                        updateUIThread(activity);
                    }

                    // Extra check to see if date folder is empty
                    // Get the files from directory
                    String path = context.get().getFilesDir()  + "/" + newspaperFolderName;
                    File checkThisFolder = new File (path);
                    if (checkThisFolder.exists())
                    {
                        File[] folders = checkThisFolder.listFiles(); // No sorting needed here
                        if (folders != null && folders.length > 0)
                        {
                            // Do nothing
                            // There are other newspaper in this date folder
                        }
                        else
                        {
                            // Delete the folder
                            if (deleteFileOrFolder(checkThisFolder, "/" + newspaperFolderName))
                                LogDAO.getInstance().add("Deleted old empty newspaper: " + newspaperFolderName);
                        }
                    }
                }
            }
        }

        String text = "Deleted " + String.valueOf(filesDeleted) + " files";
        LogDAO.getInstance().add(text);

        // Refresh log
        updateUIThread(activity);
    }

    public void checkAndDeleteOldDownloadedFilesAndFolders(String newsPaperDistributorID)
    {

        // Get the list, this can be unsorted even if folder names are sorted
        File newspaperDateFolder = context.get().getFilesDir(); // Base directory

        // Check if it actually exist ... and is a directory
        if (newspaperDateFolder.exists() && newspaperDateFolder.isDirectory()) {

            // List all folders with date
            File[] localFolderList = newspaperDateFolder.listFiles(); // No sorting needed here,  20190521, 20190522, 20190523
            if(localFolderList != null)
            {
                Calendar oneWeekAgo = Calendar.getInstance();
                oneWeekAgo.add(Calendar.DAY_OF_MONTH, -8); // One week + 1 day (same as server)

                // Check all newspaper folders in list (the format is 20190521)
                for(File currentNewspaperFolder : localFolderList)
                {
                    String newspaperFolderName = currentNewspaperFolder.getName();

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    ParsePosition pp = new ParsePosition(0);
                    Date folderDate = sdf.parse(newspaperFolderName, pp);

                    // Only delete newspapers more than one week old
                    if (folderDate != null && folderDate.getTime() < oneWeekAgo.getTimeInMillis() )
                    {
                        String fullFolderPath = context.get().getFilesDir()  + "/" + newspaperFolderName + "/" + newsPaperDistributorID;

                        // Get the files from directory
                        File dir = new File (fullFolderPath);

                        // Extra check because the newspaper distributor doesn't have to exist here with new folder system
                        if (dir.exists())
                        {
                            File[] filesInDir = dir.listFiles(); // No sorting needed here
                            if (filesInDir != null)
                            {
                                // Loop through all files
                                for(File currentFile : filesInDir)
                                {
                                    // Delete the mp3 file in the folder
                                    deleteFileOrFolder(currentFile, "/" + newspaperFolderName  + "/" + newsPaperDistributorID + "/" + currentFile.getName());
                                }
                            }

                            // The folder must be empty to delete it, that's why we needed to loop through all files and delete them
                            // This is the folder with date, for example "20180328/01/"
                            if (deleteFileOrFolder(dir, "/" + newspaperFolderName + "/" + newsPaperDistributorID))
                                LogDAO.getInstance().add("Deleted old newspaper ID: " + newsPaperDistributorID + " from: " + newspaperFolderName);
                        }

                        // Extra check to see if date folder is empty
                        // Get the files from directory
                        String path = context.get().getFilesDir()  + "/" + newspaperFolderName;
                        File checkThisFolder = new File (path);
                        if (checkThisFolder.exists())
                        {
                            File[] folders = checkThisFolder.listFiles(); // No sorting needed here
                            if (folders != null && folders.length > 0)
                            {
                                // Do nothing
                                // Other newspaper still exist in this date folder
                            }
                            else
                            {
                                // Delete the folder
                                if (deleteFileOrFolder(checkThisFolder, "/" + newspaperFolderName))
                                    LogDAO.getInstance().add("Deleted old empty newspaper: " + newspaperFolderName);
                            }
                        }
                    }
                }
            }
        }
    }

    private synchronized boolean deleteFileOrFolder(File fileToDelete, String readablePathForLog)
    {
        // Check if we can delete the file or folder
        if (fileToDelete != null && fileToDelete.exists())
        {
            if (fileToDelete.delete())
            {
                // Success
                //String fileMsg = "Deleted " + readablePathForLog;
                //LogDAO.getInstance().add(fileMsg);
                return true;
            }
            else
            {
                // Failure

                // Only write to log if failed
                String fileMsg = "Failed to delete " + readablePathForLog;
                LogDAO.getInstance().add(fileMsg);
            }
        }

        // Failure
        return false;
    }


    public synchronized boolean fileExist(String fullPath)
    {
        // Check file name and if the file exist
        File file = new File(fullPath);
        if (file != null && file.exists() && file.isFile())
            return true;
        else
            return false;
    }



    // THIS FUNCTION SHOULD BE RUN FROM ASYNC THREAD
    public void updateUIThread(Activity activity) {
        // This function sends a broadcast to the UI thread and refreshes the log list
        // You need to do it this way because you are not allowed to update the UI thread directly from async thread
        if(activity == null)
            return;

        Intent intent = new Intent(activity.getString(R.string.broadcastUpdateUIToSettingsActivity));
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES); //If set, this intent will always match any components in packages that are currently stopped.
        intent.putExtra("FSTF_UpdateLog_KEY", true);
        LocalBroadcastManager.getInstance(activity).sendBroadcast(intent);
    }
}
