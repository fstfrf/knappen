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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class FTPConnectionManager {

    private static final String TAG = "FTPConnectionManager";
    private WeakReference<Context> mContext = null;

    // Make it a static class (only one instance) | same as singleton
    private static FTPConnectionManager instance = new FTPConnectionManager();
    public static synchronized FTPConnectionManager getInstance() {
        return instance;
    }

    // This must be set before accessing context
    public void setApplicationContext(Context cx) {
        mContext = new WeakReference<>(cx);
    }

    public synchronized boolean isOnline()
    {
        if(mContext == null || mContext.get() == null)
            return false;

        ConnectivityManager connMgr = (ConnectivityManager)mContext.get().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());

        // https://developer.android.com/training/basics/network-ops/managing

        //boolean isWifiConn = false;
        //boolean isMobileConn = false;
        //for (Network network : connMgr.getAllNetworks()) {
        //    NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
        //    if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
        //        isWifiConn |= networkInfo.isConnected();
        //    }
        //    if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
        //        isMobileConn |= networkInfo.isConnected();
        //    }
        //}
    }

    public synchronized boolean testConnection(NewspaperDistributor distributor)
    {
        FTPClient connection = null;

        // First test if we are online
        boolean connected = isOnline();
        if(!connected)
        {
            LogDAO.getInstance().add("Application has no internet connection.");
            System.err.println("Application has no internet connection.");
            // Don't run Toast here, it will crash - we need a thread that has not called Looper.prepare() in that case
            // Toast.makeText(mContext.get(), "Connection failed! See log for details.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if(distributor!=null)
        {
            String ftpAddress = distributor.getFtpAddress();
            int ftpPort = distributor.getFtpPort();
            String ftpUsername = distributor.getFtpUsername();
            String ftpPassword = distributor.getFtpPassword();

            // MARK - main reason it may crash here is if you call this function from main thread. This must always be called from ASYNC!
            connection = connectToFTP(ftpAddress, ftpPort, ftpUsername, ftpPassword);
        }

        if (connection!=null)
        {
            // Ok, the connection was successfully established
            disconnectFromFTP(connection);
            return true;
        }
        else {
            return false;
        }
    }

    public synchronized FTPClient connectToFTP(String server, int portNumber, String user, String password)
    {
        FTPClient ftp = null;

        try {
            // Create a new ftp client and connect
            ftp = new FTPClient();
            ftp.connect(server, portNumber);
            LogDAO.getInstance().add("Connected. Reply: " + ftp.getReplyString());

            // Immediately after connecting is the only real time you need to check the reply code (because connect is of type void).
            int replyCode = ftp.getReplyCode();

            // Check the reply code
            if (!FTPReply.isPositiveCompletion(replyCode))
            {
                ftp.disconnect();
                LogDAO.getInstance().add("FTP server refused connection.");
                System.err.println("FTP server refused connection.");
                return null;
            }

            // Log in to ftp
            if (ftp.login(user, password))
                LogDAO.getInstance().add("Logged in. Reply: " + ftp.getReplyString());
            else {
                LogDAO.getInstance().add("Failed to log in. Reply: " + ftp.getReplyString());
                return null;
            }

            // Set binary file type
            if (!ftp.setFileType(FTP.BINARY_FILE_TYPE)) // Can be chosen between ASCII_FILE_TYPE and BINARY_FILE_TYPE
            {
                LogDAO.getInstance().add("File type set failed. Reply: " + ftp.getReplyString());
                return null;
            }

            // Make sure we use local to server connection
            ftp.enterLocalPassiveMode();

            // https://commons.apache.org/proper/commons-net/apidocs/org/apache/commons/net/ftp/FTPClient.html
            // During file transfers, the data connection is busy, but the control connection is idle.
            // FTP servers know that the control connection is in use, so won't close it through lack of activity,
            // but it's a lot harder for network routers to know that the control and data connections are associated with each other.
            // Some routers may treat the control connection as idle, and disconnect it if the transfer over the data connection takes longer than the allowable idle time for the router.
            // One solution to this is to send a safe command (i.e. NOOP) over the control connection to reset the router's idle timer. This is enabled as follows:
            // This will cause the file upload/download methods to send a NOOP approximately every 5 minutes
            ftp.setControlKeepAliveTimeout(300); // set timeout to 5 minutes


        } catch( IOException e)
        {
            Log.d(TAG, e.toString());
            LogDAO.getInstance().add(e.toString());
            return null;
        }

        return ftp;
    }

    public synchronized void disconnectFromFTP(FTPClient client)
    {
        try
        {
            // Log out and disconnect if we've got a connection
            if (client != null)
            {
                if (client.isConnected())
                {
                    client.logout();
                    client.disconnect();
                    client = null;
                }
            }
        }
        catch (IOException e)
        {
            Log.d(TAG, e.toString());
            LogDAO.getInstance().add("IOException: " + e.toString());
        }
    }
}
