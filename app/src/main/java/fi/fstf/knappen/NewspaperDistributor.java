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

// This class is a Getter/Setter for the NewspaperDistributor
public class NewspaperDistributor {

    private long    id;                         // Database id
    private int     downloadTimeHour;           // Hour for download time
    private int     downloadTimeMinute;         // Minute for download time
    private int     autoDownloadEnabled;        // Should we call the recurring download
    private long    autoDownloadInterval;       // Download interval (for example AlarmManager.INTERVAL_DAY)
    private String  ftpAddress;                 // IP address or url
    private String  ftpUsername;                // Username
    private String  ftpPassword;                // Password
    private int     ftpPort;                    // for example 21
    private int     maxNumNewspapers;           // Maximum number of newspaper saved locally
    private String  newspaperDistributorName;   // Name of the newspaper distributor ("Österbottens Tidning")
    private String  folderName;                 // The name of the folder where the Newspapers are stored ("01")
    private int     sortID;                     // The if of the list 1 = most prio

    public NewspaperDistributor()
    {
        super();
        this.id                         = 0;
        this.downloadTimeHour           = 0;
        this.downloadTimeMinute         = 0;
        this.autoDownloadEnabled        = 0;
        this.autoDownloadInterval       = AlarmManager.INTERVAL_DAY;    // Default to day
        this.ftpAddress                 = "";
        this.ftpUsername                = "";
        this.ftpPassword                = "";
        this.ftpPort                    = 0;
        this.maxNumNewspapers           = 0;
        this.newspaperDistributorName   = "";
        this.folderName                 = "";
        this.sortID                     = 1;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getDownloadTimeHour() {
        return downloadTimeHour;
    }

    public void setDownloadTimeHour(int hour) {
        this.downloadTimeHour = hour;
    }

    public int getDownloadTimeMinute() {
        return downloadTimeMinute;
    }

    public void setDownloadTimeMinute(int minute) {
        this.downloadTimeMinute = minute;
    }

    public int getAutoDownloadEnabled() {
        return autoDownloadEnabled;
    }

    public void setAutoDownloadEnabled(int enabled) {
        this.autoDownloadEnabled = enabled;
    }

    public long getAutoDownloadInterval() {
        return autoDownloadInterval;
    }

    public void setAutoDownloadInterval(long interval) {
        this.autoDownloadInterval = interval;
    }

    public String getFtpAddress() {
        return ftpAddress;
    }

    public void setFtpAddress(String ftpAddress) {
        this.ftpAddress = ftpAddress;
    }

    public String getFtpUsername() {
        return ftpUsername;
    }

    public void setFtpUsername(String ftpUsername) {
        this.ftpUsername = ftpUsername;
    }

    public String getFtpPassword() {
        return ftpPassword;
    }

    public void setFtpPassword(String ftpPassword) {
        this.ftpPassword = ftpPassword;
    }

    public int getFtpPort() {
        return ftpPort;
    }

    public void setFtpPort(int ftpPort) {
        this.ftpPort = ftpPort;
    }

    public int getMaxNumNewspapers() {
        return maxNumNewspapers;
    }

    public void setMaxNumNewspapers(int maxNumNewspapers) {
        this.maxNumNewspapers = maxNumNewspapers;
    }

    public String getNewspaperDistributorName() {
        return newspaperDistributorName;
    }

    public void setNewspaperDistributorName(String newspaperDistributorName) {
        this.newspaperDistributorName = newspaperDistributorName;
    }

    public synchronized String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public int getSortID() {
        return sortID;
    }

    public void setSortID(int newSortID) {
        this.sortID = newSortID;
    }
}
