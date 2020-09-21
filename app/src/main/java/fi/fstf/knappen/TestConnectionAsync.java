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
import android.os.AsyncTask;
import android.widget.Toast;

import java.lang.ref.WeakReference;

// This class will test FTP connection async
public class TestConnectionAsync extends AsyncTask<String,String,String> {

    private String mNewspaperDistributorFolderID = "";
    private WeakReference<Context> mContext;

    public TestConnectionAsync(Context cx, String distributorFolderID) {
        this.mNewspaperDistributorFolderID = distributorFolderID; // For example "01"
        this.mContext = new WeakReference<>(cx);
    }

    // https://developer.android.com/reference/android/os/AsyncTask.html
    protected String doInBackground(String... params) {

        if (mNewspaperDistributorFolderID.equals(""))
        {
            // Here we could do some more error handling if we want to
            return "fail";
        }

        NewspaperDistributor distributor = NewspaperDistributorDAO.getInstance().getNewspaperDistributor(mNewspaperDistributorFolderID);

        // The FTP connection needs to be ASYNC!
        FTPConnectionManager.getInstance().setApplicationContext(mContext.get());
        boolean success = FTPConnectionManager.getInstance().testConnection(distributor);

        // The return value here will go into onPostExecute as result
        if (success)
            return "ok";
        else
            return "fail";
    }

    protected void onProgressUpdate(String... progress) {
        //setProgressPercent(progress[0]);
    }

    protected void onPostExecute(String result) {

        // We will get here when doInBackground() is done
        // The result value will be "ok" or "fail" returned from doInBackground(), but we don't need that at the moment
        if (result.equals("ok"))
            Toast.makeText(mContext.get(), "Connection ok!", Toast.LENGTH_SHORT).show();
        else if (result.equals("fail"))
            Toast.makeText(mContext.get(), "Connection failed! See log for details.", Toast.LENGTH_SHORT).show();
        else
        {
            Toast.makeText(mContext.get(), "Unknown result!", Toast.LENGTH_SHORT).show();
        }

    }
}