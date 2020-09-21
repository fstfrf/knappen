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
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

// This class will delete all files asynchronously (will be called manually from Settings)
public class DeleteFilesAsync extends AsyncTask<String,String,String> {

    private WeakReference<Activity> mActivity;
    private boolean mDeleteDistributor = false;
    private String mNewspaperDistributorFolderID = "";

    public DeleteFilesAsync(Activity activity, boolean deleteDistributor, String distributorFolderID) {
        if(activity!= null)
            this.mActivity = new WeakReference<>(activity);
        this.mDeleteDistributor = deleteDistributor;
        this.mNewspaperDistributorFolderID = distributorFolderID; // For example "01"
    }

    // https://developer.android.com/reference/android/os/AsyncTask.html
    protected String doInBackground(String... params) {

        if (mNewspaperDistributorFolderID.equals(""))
        {
            // Here we could do some more error handling if we want to
            return "fail";
        }

        if(mActivity != null && mActivity.get() != null)
        {
            // Always delete all mp3 files and folders
            FileManager.getInstance().deleteAllDownloadedFilesAndFolders(mActivity.get(), mNewspaperDistributorFolderID);
        }
        else
        {
            LogDAO.getInstance().add("Failed to delete downloaded folder because of no Activity set!");
        }

        // Should we delete the distributor as well?
        if (mDeleteDistributor)
        {
            // Delete the saved data for the distributor
            int numRowsAffected = NewspaperDistributorDAO.getInstance().deleteDistributorWithFolderID(mNewspaperDistributorFolderID);

            if (numRowsAffected > 0)
                LogDAO.getInstance().add("Deleted distributor with ID :" + mNewspaperDistributorFolderID);
            else
                LogDAO.getInstance().add("No distributor to delete!");
        }

        // The return value here will go into onPostExecute as result
        return "ok";
    }

    protected void onProgressUpdate(String... progress) {
        //setProgressPercent(progress[0]);
    }

    protected void onPostExecute(String result) {

        // We will get here when doInBackground() is done
        // The result value will be "ok" or "fail" returned from doInBackground(), but we don't need that at the moment

        if(mActivity != null && mActivity.get() != null)
        {
            // Send Broadcast to Broadcast receiver with message to update UI
            // This means we will only update UI once (not for every delete above)
            FileManager.getInstance().updateUIThread(mActivity.get());
        }
    }
}