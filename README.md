# Knappen

Copyright 2020 Finlands svenska taltidningsförening rf

Knappen is an audio newspaper app designed for the visually impaired and other reading disabled. The Android app has been developed by Finlands svenska taltidningsförening rf (FSTF, www.fstf.fi) and is licensed under the Apache 2.0 open source license (see file LICENSE for more information).

Knappen enables the end user to easily play newspapers in audio format (mp3). The newspapers are automatically downloaded from FTP-services at a time selected by an admin and when the end user pushes the screen of the Android phone for 0,3 to 3 seconds, the playback of the latest newspaper starts. 

By pushing the screen for more than 3 seconds, the player jumps to the next audio file. By pushing the screen for more than 6 seconds, the player jumps to the next audio file. By pushing the screen for more than 10 seconds, the player jumps to the previous folder. 

The newspapers are to be sorted into folders with the date of the newspaper as the folder name (YYYYMMDD). The files in the folders are played by their alphanumerical sorting order. 

To access the admin menu in Knappen, push the screen 12 times in the following pattern: clockwise from the upper left corner, one time in each corner of the screen. 

In the admin menu, FTP-services can be added and sorted. There is also possibility to set the reading speed of the audio.

Knappen is designed to be used with an app enabling kiosk mode (for instance Fully Kiosk), which prevents access to other features on an Android phone besides changing the playback volume and shutting down. 

Any questions can be directed to info@fstf.fi

## Building an app

The configuration of the software in this repository is the version FSTF is developing and using. If this software is to be used for other purposes, FSTF would be happy if the "applicationId" in the file /app/build.gradle (including the java package name) would be changed to something else than "fi.fstf.knappen". The app is not available at Google Play (for now).

Otherwise, building the app in Android Studio should be straightforward.
