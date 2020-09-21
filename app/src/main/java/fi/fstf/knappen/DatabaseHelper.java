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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// This is the DatabaseHelper class with all tables, and helper functions needed
public class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper sInstance;
    private SQLiteDatabase database;

    public static synchronized DatabaseHelper getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    public synchronized SQLiteDatabase getDatabase()
    {
        return sInstance.getWritableDatabase();
    }

    // TABLES
    public static final String TABLE_LOG                            = "log";
    public static final String TABLE_NEWSPAPERDISTRIBUTOR           = "newspaperdistributor";

    // THESE CAN BE SHARED ALONG TABLES
    public static final String COLUMN_NAME_ENTRY_ID                 = "_id";
    public static final String COLUMN_NAME_TIMESTAMP                = "timestamp";
    public static final String COLUMN_NAME_TEXT                     = "text";

    public static final String COLUMN_NAME_DOWNLOAD_TIME_HOUR       = "downloadtimehour";
    public static final String COLUMN_NAME_DOWNLOAD_TIME_MINUTE     = "downloadtimeminute";
    public static final String COLUMN_NAME_AUTO_DOWNLOAD_ENABLED    = "autodownloadenabled";
    public static final String COLUMN_NAME_AUTO_DOWNLOAD_INTERVAL   = "autodownloadinterval";
    public static final String COLUMN_NAME_FTP_ADDRESS              = "ftpaddress";
    public static final String COLUMN_NAME_FTP_USERNAME             = "ftpusername";
    public static final String COLUMN_NAME_FTP_PASSWORD             = "ftppassword";
    public static final String COLUMN_NAME_FTP_PORT                 = "ftpport";
    public static final String COLUMN_NAME_MAX_NUM_NEWSPAPERS       = "maxnumnewspapers";
    public static final String COLUMN_NAME_DISTRIBUTOR_NAME         = "distributorname";
    public static final String COLUMN_NAME_FOLDER_NAME              = "foldername";
    public static final String COLUMN_NAME_SORT_ID                  = "sortid";

    // https://www.sqlite.org/datatype3.html
    private static final String TEXT_TYPE   = " TEXT";
    private static final String INT_TYPE    = " INTEGER";     // Can be 64 bit
    private static final String DATE_TYPE   = " DATE";
    private static final String BOOL_TYPE   = " BOOLEAN";
    private static final String FLOAT_TYPE  = " REAL";
    private static final String DOUBLE_TYPE = " DOUBLE";
    private static final String COMMA_SEP   = ",";

    // CREATE THE LOG TABLE
    private static final String SQL_CREATE_TABLE_LOG =
            "CREATE TABLE IF NOT EXISTS " + TABLE_LOG + " (" +
                    COLUMN_NAME_ENTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_NAME_TIMESTAMP + DATE_TYPE + COMMA_SEP +
                    COLUMN_NAME_TEXT + TEXT_TYPE +  // LAST ONE WITHOUT COMMA
                    " )";

    // CREATE THE NEWSPAPERDISTRIBUTION TABLE
    private static final String SQL_CREATE_TABLE_NEWSPAPERDISTRIBUTOR =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NEWSPAPERDISTRIBUTOR + " (" +
                    COLUMN_NAME_ENTRY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_NAME_DOWNLOAD_TIME_HOUR + INT_TYPE + COMMA_SEP +
                    COLUMN_NAME_DOWNLOAD_TIME_MINUTE + INT_TYPE + COMMA_SEP +
                    COLUMN_NAME_AUTO_DOWNLOAD_ENABLED + BOOL_TYPE + COMMA_SEP +
                    COLUMN_NAME_AUTO_DOWNLOAD_INTERVAL + INT_TYPE + COMMA_SEP +
                    COLUMN_NAME_FTP_ADDRESS + TEXT_TYPE + COMMA_SEP +
                    COLUMN_NAME_FTP_USERNAME + TEXT_TYPE + COMMA_SEP +
                    COLUMN_NAME_FTP_PASSWORD + TEXT_TYPE + COMMA_SEP +
                    COLUMN_NAME_FTP_PORT + INT_TYPE + COMMA_SEP +
                    COLUMN_NAME_MAX_NUM_NEWSPAPERS + INT_TYPE + COMMA_SEP +
                    COLUMN_NAME_DISTRIBUTOR_NAME + TEXT_TYPE + COMMA_SEP +
                    COLUMN_NAME_FOLDER_NAME + TEXT_TYPE + COMMA_SEP +
                    COLUMN_NAME_SORT_ID + INT_TYPE + // LAST ONE WITHOUT COMMA
                    " )";

    // Every time you change the database schema, you must increment the database version here
    public static final int     DATABASE_VERSION    = 4;
    public static final String  DATABASE_NAME       = "FSTF.db";

     /**
     * Constructor should be private to prevent direct instantiation.
     * make call to static method "getInstance()" instead.
     */
    private DatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {

        db.execSQL(SQL_CREATE_TABLE_LOG);
        db.execSQL(SQL_CREATE_TABLE_NEWSPAPERDISTRIBUTOR);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        switch(oldVersion)
        {
            // Do not use breaks here because we want all updates if someone have got an very old version.

            case 1:
                // v1 -> v2: Changed - added the TABLE_NEWSPAPERDISTRIBUTOR, if updating from v0.5 (or earlier) to v0.6 you will need this
            case 2:
                // v2 -> v3: Changed - changes in TABLE_NEWSPAPERDISTRIBUTOR -> must delete and create new empty if updated to this version
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NEWSPAPERDISTRIBUTOR);
                db.execSQL(SQL_CREATE_TABLE_NEWSPAPERDISTRIBUTOR);
            case 3:
                // v3 -> v4: When changed to version 4, oldversion will be 3.
                // Write comment here what you have changed and make sure you do proper setup to not crash old versions
            case 4:
                // v4 -> v5 ...
                // New in database v4 (May 2019)
                db.execSQL("ALTER TABLE " + TABLE_NEWSPAPERDISTRIBUTOR  + " ADD COLUMN " + COLUMN_NAME_SORT_ID + " INTEGER DEFAULT 1");
        }
    }

}
