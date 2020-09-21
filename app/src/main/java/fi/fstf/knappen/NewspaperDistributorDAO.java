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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

import java.util.ArrayList;
import java.util.List;

// This is the DAO for our NewspaperDistributor
public class NewspaperDistributorDAO {

    // Make sure we only make one DAO for the Newspaper Distributor
    private static NewspaperDistributorDAO ourInstance = new NewspaperDistributorDAO();
    public static synchronized NewspaperDistributorDAO getInstance() {
        return ourInstance;
    }

    // Database fields
    private DatabaseHelper dbHelper = null;
    private String[] allColumns =
            {
                    DatabaseHelper.COLUMN_NAME_ENTRY_ID,
                    DatabaseHelper.COLUMN_NAME_DOWNLOAD_TIME_HOUR,
                    DatabaseHelper.COLUMN_NAME_DOWNLOAD_TIME_MINUTE,
                    DatabaseHelper.COLUMN_NAME_AUTO_DOWNLOAD_ENABLED,
                    DatabaseHelper.COLUMN_NAME_AUTO_DOWNLOAD_INTERVAL,
                    DatabaseHelper.COLUMN_NAME_FTP_ADDRESS,
                    DatabaseHelper.COLUMN_NAME_FTP_USERNAME,
                    DatabaseHelper.COLUMN_NAME_FTP_PASSWORD,
                    DatabaseHelper.COLUMN_NAME_FTP_PORT,
                    DatabaseHelper.COLUMN_NAME_MAX_NUM_NEWSPAPERS,
                    DatabaseHelper.COLUMN_NAME_DISTRIBUTOR_NAME,
                    DatabaseHelper.COLUMN_NAME_FOLDER_NAME,
                    DatabaseHelper.COLUMN_NAME_SORT_ID
            };

    private NewspaperDistributorDAO() {}

    public void createDB(Context context) throws SQLException {
        if (dbHelper==null)
            dbHelper = DatabaseHelper.getInstance(context);
    }

    public void close() {
        dbHelper.close();
    }

    public NewspaperDistributor add(int downloadTimeHour, int downloadTimeMinute, int autoDownloadEnabled, long autoDownloadInterval, String ftpAddress, String ftpUsername, String ftpPw, int ftpPort, int maxNumNewspapers, String name, String folderName, int sortID)
    {
        // Set the value for database
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME_DOWNLOAD_TIME_HOUR, downloadTimeHour);
        values.put(DatabaseHelper.COLUMN_NAME_DOWNLOAD_TIME_MINUTE, downloadTimeMinute);
        values.put(DatabaseHelper.COLUMN_NAME_AUTO_DOWNLOAD_ENABLED, autoDownloadEnabled);
        values.put(DatabaseHelper.COLUMN_NAME_AUTO_DOWNLOAD_INTERVAL, autoDownloadInterval);
        values.put(DatabaseHelper.COLUMN_NAME_FTP_ADDRESS, ftpAddress);
        values.put(DatabaseHelper.COLUMN_NAME_FTP_USERNAME, ftpUsername);
        values.put(DatabaseHelper.COLUMN_NAME_FTP_PASSWORD, ftpPw);
        values.put(DatabaseHelper.COLUMN_NAME_FTP_PORT, ftpPort);
        values.put(DatabaseHelper.COLUMN_NAME_MAX_NUM_NEWSPAPERS, maxNumNewspapers);
        values.put(DatabaseHelper.COLUMN_NAME_DISTRIBUTOR_NAME, name);
        values.put(DatabaseHelper.COLUMN_NAME_FOLDER_NAME, folderName);
        values.put(DatabaseHelper.COLUMN_NAME_SORT_ID, sortID);

        long insertId = dbHelper.getDatabase().insert(DatabaseHelper.TABLE_NEWSPAPERDISTRIBUTOR, null, values);
        Cursor cursor = dbHelper.getDatabase().query(DatabaseHelper.TABLE_NEWSPAPERDISTRIBUTOR, allColumns, DatabaseHelper.COLUMN_NAME_ENTRY_ID + " = " + insertId, null, null, null, null);
        cursor.moveToFirst();
        NewspaperDistributor newItem = cursorToItem(cursor);
        cursor.close();
        return newItem;
    }

    public boolean updateDistributorSorting(String distributorFolderID, int newSortID)
    {
        ContentValues value = new ContentValues();
        value.put(DatabaseHelper.COLUMN_NAME_SORT_ID, newSortID);

        // Update the database if the uID exist (could have been removed from list already)
        int rowsAffected = dbHelper.getDatabase().update(DatabaseHelper.TABLE_NEWSPAPERDISTRIBUTOR, value, DatabaseHelper.COLUMN_NAME_FOLDER_NAME + "='" + distributorFolderID + "'",null);

        if (rowsAffected == 0)
            return false;
        else
            return true;
    }

    public boolean updateDistributor(int downloadTimeHour, int downloadTimeMinute, int autoDownloadEnabled, long autoDownloadInterval, String ftpAddress, String ftpUsername, String ftpPassword, int ftpPort, int maxNumNewspapers, String distributorName, String distributorFolderID){

        // The folder ID must be set ... that is our ID
        if (distributorFolderID == null || distributorFolderID.isEmpty())
            return false;

        ContentValues value = new ContentValues();
        value.put(DatabaseHelper.COLUMN_NAME_DOWNLOAD_TIME_HOUR, downloadTimeHour);
        value.put(DatabaseHelper.COLUMN_NAME_DOWNLOAD_TIME_MINUTE, downloadTimeMinute);
        value.put(DatabaseHelper.COLUMN_NAME_AUTO_DOWNLOAD_ENABLED, autoDownloadEnabled);
        value.put(DatabaseHelper.COLUMN_NAME_AUTO_DOWNLOAD_INTERVAL, autoDownloadInterval);
        value.put(DatabaseHelper.COLUMN_NAME_FTP_ADDRESS, ftpAddress);
        value.put(DatabaseHelper.COLUMN_NAME_FTP_USERNAME, ftpUsername);
        value.put(DatabaseHelper.COLUMN_NAME_FTP_PASSWORD, ftpPassword);
        value.put(DatabaseHelper.COLUMN_NAME_FTP_PORT, ftpPort);
        value.put(DatabaseHelper.COLUMN_NAME_MAX_NUM_NEWSPAPERS, maxNumNewspapers);
        value.put(DatabaseHelper.COLUMN_NAME_DISTRIBUTOR_NAME, distributorName);

        // Update the database if the uID exist (could have been removed from list already)
        int rowsAffected = dbHelper.getDatabase().update(DatabaseHelper.TABLE_NEWSPAPERDISTRIBUTOR, value, DatabaseHelper.COLUMN_NAME_FOLDER_NAME + "='" + distributorFolderID + "'",null);

        if (rowsAffected == 0)
            return false;
        else
            return true;
    }

    public int deleteDistributorWithFolderID(String distributorFolderID)
    {
        return dbHelper.getDatabase().delete(DatabaseHelper.TABLE_NEWSPAPERDISTRIBUTOR, DatabaseHelper.COLUMN_NAME_FOLDER_NAME + "='" + distributorFolderID + "'", null);
    }

    public synchronized int getNextID()
    {
        String query = "SELECT MAX(_id) AS max_id FROM newspaperdistributor";
        Cursor cursor = dbHelper.getDatabase().rawQuery(query, null);

        int id = 0;
        if (cursor.moveToFirst())
        {
            do
            {
                id = cursor.getInt(0);
            } while(cursor.moveToNext());
        }

        // Add one to get the next
        id += 1;

        cursor.close();
        return id;
    }

    public synchronized NewspaperDistributor getNewspaperDistributor(String folderID) {

        if (dbHelper.getDatabase() == null)
            return null;

        NewspaperDistributor newspaperDistributor = null;
        Cursor cursor = dbHelper.getDatabase().query(DatabaseHelper.TABLE_NEWSPAPERDISTRIBUTOR, allColumns, DatabaseHelper.COLUMN_NAME_FOLDER_NAME + "='" + folderID + "'", null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            newspaperDistributor = cursorToItem(cursor);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return newspaperDistributor;
    }

    public synchronized List<NewspaperDistributor> getAllNewspaperDistributorsSorted() {

        if (dbHelper.getDatabase() == null)
            return null;

        List<NewspaperDistributor> newspaperDistributorList = new ArrayList<NewspaperDistributor>();

        Cursor cursor = dbHelper.getDatabase().query(DatabaseHelper.TABLE_NEWSPAPERDISTRIBUTOR, allColumns, null, null, null, null, DatabaseHelper.COLUMN_NAME_SORT_ID);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            NewspaperDistributor item = cursorToItem(cursor);
            newspaperDistributorList.add(item);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return newspaperDistributorList;
    }

    private NewspaperDistributor cursorToItem(Cursor cursor) {
        int cursorID = 0;
        NewspaperDistributor newItem = new NewspaperDistributor();
        newItem.setId(cursor.getLong(cursorID));                            cursorID++;
        newItem.setDownloadTimeHour(cursor.getInt(cursorID));               cursorID++;
        newItem.setDownloadTimeMinute(cursor.getInt(cursorID));             cursorID++;
        newItem.setAutoDownloadEnabled(cursor.getInt(cursorID));            cursorID++;
        newItem.setAutoDownloadInterval(cursor.getLong(cursorID));          cursorID++;
        newItem.setFtpAddress(cursor.getString(cursorID));                  cursorID++;
        newItem.setFtpUsername(cursor.getString(cursorID));                 cursorID++;
        newItem.setFtpPassword(cursor.getString(cursorID));                 cursorID++;
        newItem.setFtpPort(cursor.getInt(cursorID));                        cursorID++;
        newItem.setMaxNumNewspapers(cursor.getInt(cursorID));               cursorID++;
        newItem.setNewspaperDistributorName(cursor.getString(cursorID));    cursorID++;
        newItem.setFolderName(cursor.getString(cursorID));                  cursorID++;
        newItem.setSortID(cursor.getInt(cursorID));                         cursorID++;
        return newItem;
    }
}
