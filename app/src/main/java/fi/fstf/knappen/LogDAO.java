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
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

// The DAO class for our Log
public class LogDAO {

    // Good source used in several projects
    // http://www.vogella.com/tutorials/AndroidSQLite/article.html

    // Make sure we only make one DAO for the Log
    private static LogDAO ourInstance = new LogDAO();
    public static synchronized LogDAO getInstance() {
        return ourInstance;
    }

    // Database fields
    private DatabaseHelper dbHelper = null;
    private String[] allColumns =
            {
                    DatabaseHelper.COLUMN_NAME_ENTRY_ID,
                    DatabaseHelper.COLUMN_NAME_TIMESTAMP,
                    DatabaseHelper.COLUMN_NAME_TEXT
            };

    public LogDAO() {}

    public void createDB(Context context) throws SQLException {
        if (dbHelper==null)
            dbHelper = DatabaseHelper.getInstance(context);
    }

    public void close() {
        dbHelper.close();
    }

    // Add item to log
    public synchronized LogItem add(String text)
    {
        // Always log to android system, use i = info as type (debug will be stripped out from release build)
        Log.i("FSTF", text);

        if (dbHelper.getWritableDatabase() == null)
            return null;

        // DATE AND TIME
        Calendar c = Calendar.getInstance();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME_TIMESTAMP, c.getTimeInMillis());
        values.put(DatabaseHelper.COLUMN_NAME_TEXT, text);

        long insertId = dbHelper.getWritableDatabase().insert(DatabaseHelper.TABLE_LOG, null, values);
        Cursor cursor = dbHelper.getWritableDatabase().query(DatabaseHelper.TABLE_LOG, allColumns, DatabaseHelper.COLUMN_NAME_ENTRY_ID + " = " + insertId, null, null, null, null);
        cursor.moveToFirst();
        LogItem newItem = cursorToItem(cursor);
        cursor.close();
        return newItem;
    }

    public void deleteAllLogItems()
    {
        dbHelper.getWritableDatabase().delete(DatabaseHelper.TABLE_LOG, "1", null);
    }

    public List<LogItem> getAllLogItems()
    {
        List<LogItem> logItemList = new ArrayList<LogItem>();

        Cursor cursor = dbHelper.getWritableDatabase().query(DatabaseHelper.TABLE_LOG, allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            LogItem item = cursorToItem(cursor);
            logItemList.add(item);
            cursor.moveToNext();
        }

        // make sure to close the cursor
        cursor.close();
        return logItemList;
    }

    // Read data from cursor
    private LogItem cursorToItem(Cursor cursor) {
        int cursorID = 0;
        LogItem newItem = new LogItem();
        newItem.setId(cursor.getLong(cursorID));         cursorID++;
        newItem.setTimestamp(cursor.getLong(cursorID));  cursorID++;
        newItem.setText(cursor.getString(cursorID));     cursorID++;
        return newItem;
    }
}