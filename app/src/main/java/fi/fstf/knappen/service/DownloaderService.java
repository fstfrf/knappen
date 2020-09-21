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
package fi.fstf.knappen.service;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import fi.fstf.knappen.DownloadFilesAsync;
import fi.fstf.knappen.LogDAO;

// This is the DownloaderService class that will be triggered by the AlarmReceiver
public class DownloaderService extends Service {
    public DownloaderService() {
    }

    @Override
    public IBinder onBind(Intent intent) {

        // We will not use binding
        LogDAO.getInstance().createDB(this);
        LogDAO.getInstance().add("DownloaderService:onBind()");

        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);

        // If we have exited the app (and swipe closed), the intent will be null
        // Because of the START_STICKY below it would start downloading again if exited ...
        // but now we skip it until next time we start the app
        if (intent == null)
            return Service.START_NOT_STICKY;

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            String newspaperDistributorFolderID = bundle.getString("NEWSPAPER_DISTRIBUTOR_FOLDER_ID");

            if (!newspaperDistributorFolderID.equals(""))
            {
                LogDAO.getInstance().createDB(this);
                LogDAO.getInstance().add("DownloaderService:onStartCommand()");

                // Use the DownloaderService as context
                // The activity will be null here, if activity is set the class will know to update the UI log
                // But this is done in the background now and we don't need to see what's happening
                new DownloadFilesAsync(null, this, newspaperDistributorFolderID, false).execute();

                // Remember to delete the intent
                intent.removeExtra("NEWSPAPER_DISTRIBUTOR_FOLDER_ID");
            }
            else
            {
                LogDAO.getInstance().createDB(this);
                LogDAO.getInstance().add("DownloaderService:onStartCommand() error - no folder ID set");
            }
        }
        else
        {

            LogDAO.getInstance().createDB(this);
            LogDAO.getInstance().add("DownloaderService:onStartCommand() error - bundle is null");
        }

        return Service.START_STICKY;
    }
}
