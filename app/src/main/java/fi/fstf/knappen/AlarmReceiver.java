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
import android.os.Bundle;

import fi.fstf.knappen.service.DownloaderService;

// This is the AlarmReceiver class that will get the alarm from Android when time is set by support admin
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (context != null)
        {
            if(intent != null) {

                // Check if we have any extras (should be!)
                Bundle bundle = intent.getExtras();
                if (bundle != null) {

                    // Check if we got extra data
                    String newspaperDistributorFolderID = bundle.getString("NEWSPAPER_DISTRIBUTOR_FOLDER_ID");

                    if (!newspaperDistributorFolderID.equals(""))
                    {
                        // Log to DB
                        LogDAO.getInstance().createDB(context);
                        LogDAO.getInstance().add("AlarmReceiver:onReceive() - requesting download service for folder: " + newspaperDistributorFolderID);

                        // Start the download
                        Intent downloader = new Intent(context, DownloaderService.class);
                        downloader.putExtra("NEWSPAPER_DISTRIBUTOR_FOLDER_ID", newspaperDistributorFolderID);
                        context.startService(downloader);

                        // Remember to remove extra
                        intent.removeExtra("NEWSPAPER_DISTRIBUTOR_FOLDER_ID");
                    }
                    else
                    {
                        // Log to DB
                        LogDAO.getInstance().createDB(context);
                        LogDAO.getInstance().add("AlarmReceiver:onReceive() error - no folder ID set!");
                    }
                }
            }
        }
    }
}
