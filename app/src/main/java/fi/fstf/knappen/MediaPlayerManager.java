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
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.IOException;
import java.lang.ref.WeakReference;

// This is the MediaPlayerManager class that handles all the code for Android MediaPlayer (could be changed to other player in here if wanted)
public class MediaPlayerManager implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    private MediaPlayer     mediaPlayer = null;
    private WeakReference<Context> context = null;
    private boolean         isEnabled = false;
    private float           audio_speed = 1.0f;
    private float           audio_pitch = 1.0f;
    private int             currentPosition = 0;        // milliseconds into the media (used for play/pause)
    private boolean         shouldPauseAfterCompletion = true;
    private boolean         voicePlaying = false;

    // Make it a static class (only one instance) | same as singleton
    private static MediaPlayerManager instance = new MediaPlayerManager();
    public static synchronized MediaPlayerManager getInstance() {
        return instance;
    }

    // This must be set before accessing context
    public void setApplicationContext(Context cx)
    {
        context = new WeakReference<>(cx);

        // First time the application is run these values will be set to default. Else they will use the values set in settings
        SharedPreferences sharedPref = context.get().getSharedPreferences(context.get().getString(R.string.sp_shared_preferences),Context.MODE_PRIVATE);
        audio_speed = sharedPref.getFloat(context.get().getString(R.string.sp_audio_speed), 1.0f); // 1.0f = default
        audio_pitch = sharedPref.getFloat(context.get().getString(R.string.sp_audio_pitch), 1.0f); // 1.0f = default
    }

    // START currentPosition - synchronize safe
    public synchronized void resetCurrentPosition()
    {
        setCurrentPosition(0);
    }
    public synchronized void setCurrentPosition(int value)
    {
        currentPosition = value;
    }
    public synchronized int getCurrentPosition()
    {
        return currentPosition;
    }
    // END currentPosition

    public synchronized void setEnabled(boolean value)
    {
        isEnabled = value;
    }
    public synchronized boolean isEnabled()
    {
        return isEnabled;
    }


    public void setShouldPauseAfterCompletion(boolean set) { this.shouldPauseAfterCompletion = set;}

    public boolean playReaderFiles()
    {
        if(!isEnabled())
            return false;

        // Get the uri to the current file that should be played
        Uri uri = FileManager.getInstance().getCurrentPlayableMediaFileUri();

        if (uri == null)
        {
            LogDAO.getInstance().add("Did not find file to play!");
            return false;
        }

        voicePlaying = false;

        // Start the play with the uri
        return play(uri);
    }

    public boolean play(Uri uri)
    {
        // Stop if we are playing something
        stop();

        if(!isEnabled())
            return false;

        if (uri == null)
        {
            LogDAO.getInstance().add("Did not find file to play!");
            return false;
        }

        if(context == null || context.get() == null)
        {
            LogDAO.getInstance().add("Mediaplayer::play() - context is null");
            return false;
        }

        try {
            //mediaPlayer =  MediaPlayer.create(context, uri); // This will call prepare() automatically on success
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnErrorListener(this);
            // MediaPlayer: setScreenOnWhilePlaying(true) is ineffective without a SurfaceHolder
            //mediaPlayer.setScreenOnWhilePlaying(true);

            //SetPlaybackParamsToMedia(audio_speed, audio_pitch);

            mediaPlayer.setDataSource(context.get(), uri);
            mediaPlayer.prepareAsync(); // prepare async to not block main thread - will jump into onPrepared when completed prepared async
        }
        catch (IllegalArgumentException | IOException | IllegalStateException | SecurityException e)
        {
            LogDAO.getInstance().add(e.toString());
            return false;
        }

        //mediaPlayer.start();

        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

        if(!isEnabled())
            return;

        // Check if the pointers are different
        if(mp != mediaPlayer)
        {
            LogDAO.getInstance().add("Mediaplayer does not match mp");
            stop(); // Stop the mediaplayer that was used before
            mediaPlayer = mp;
        }

        // Check if it is a newspaper media, that can be paused and played + changed speed
        if(!voicePlaying)
        {
            // Set the playback params (speed/pitch)
            setPlaybackParamsToMedia(mp, audio_speed, audio_pitch);

            // Check if we have paused it, forward to correct position
            if(getCurrentPosition()!=0)
            {
                // Request: seek to a position about 2 seconds before the current pause position
                int rewindMS = 2000; // 2 seconds
                int newSeekPos = getCurrentPosition();
                if(newSeekPos > rewindMS)
                    newSeekPos -= rewindMS;

                // Seek to the fixed position
                mp.seekTo(newSeekPos);
            }

            // Always reset
            resetCurrentPosition();
        }

        // After the object is prepared, calling it with non-zero speed is equivalent to calling start().
        mp.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

        if(!isEnabled())
            return;

        if(getCurrentPosition() == 0)
        {
            // The currentPosition will be 0 when a newspaper media file has played to its end
            // We can also get here from first touch when the media file is in stopped state

            boolean newPlayableFileFound = false;

            // Play next newspaper file in list, only if we are currently playing newspaper articles
            // If we start from beginning, a Play() will do fine!
            if(!voicePlaying)
            {
                newPlayableFileFound = FileManager.getInstance().gotoNextPlayableFile();
            }

            // Don't start playing here if we want to pause (normally it goes into the loop below where currentPosition > 0 but it can also get here,
            // for example after playing welcome message
            if(!voicePlaying)
            {
                // We are playing articles, check if we should continue
                if(newPlayableFileFound)
                    playReaderFiles();
                else
                {
                    // End of newspaper
                    stop();

                    playVoiceMessage(VoiceID.VID_YOU_HAVE_REACHED_THE_END_OF_THE_NEWSPAPER.ordinal(), true);

                    // Return here, else the booleans will be reset in the end of this function
                    return;
                }
            }
            else
            {
                // Guide voice has just been played, what should we do next...
                if(!shouldPauseAfterCompletion )
                    playReaderFiles();
                else
                {
                    // Make sure we stop ... just to be safe
                    stop();
                }
            }

        }
        else // "Pause" and "Continue" touch press will come in here when we have paused a play and the currentPosition has a saved value
        {
            if (shouldPauseAfterCompletion)
            {
                // Don't start playing anything new = pause
                if (mp != null)
                {
                    // Make sure we actually stop even if it is "pause"
                    stop();
                }
            }
            else
            {
                // If the current position has a value other than 0, pause can resume from last position
                playReaderFiles();
            }
        }

        // Always reset these here
        shouldPauseAfterCompletion = false;
        voicePlaying = false;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {

        String whatError = String.valueOf(what);
        String extraError = String.valueOf(extra);

        if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN)
            whatError = "MEDIA_ERROR_UNKNOWN";
        else if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED)
            whatError = "MEDIA_ERROR_SERVER_DIED";

        if (extra == MediaPlayer.MEDIA_ERROR_IO)
            extraError = "MEDIA_ERROR_IO";
        else if (extra == MediaPlayer.MEDIA_ERROR_MALFORMED)
            extraError = "MEDIA_ERROR_MALFORMED";
        else if (extra == MediaPlayer.MEDIA_ERROR_UNSUPPORTED)
            extraError = "MEDIA_ERROR_UNSUPPORTED";
        else if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT)
            extraError = "MEDIA_ERROR_TIMED_OUT";

        String errorMsg = "Media player onError() " + whatError + " " + extraError;
        LogDAO.getInstance().add(errorMsg);

        if (mp != null && mp.isPlaying()) {
            mp.stop();
            mp.reset();
            mp.release();

            mp = null;
            mediaPlayer = null;
        }

        // Return rue if the method handled the error, false if it didn't.
        return true;
    }

    // Take a mediaplayer reference here to make sure it works from both UI thread and async thread
    public void setPlaybackParamsToMedia(MediaPlayer mp, float speed, float pitch) {
        if (mp != null) {
            try {
                mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(speed));
            } catch (Exception e) {
                // Always write out the error
                LogDAO.getInstance().add(e.toString());
            }
        }
    }

    public void savePlaybackParams(float speed, float pitch)
    {
        // Save the local variables
        this.audio_speed = speed;
        this.audio_pitch = pitch;

        if(context == null || context.get() == null)
        {
            LogDAO.getInstance().add("Mediaplayer::savePlaybackParams() - context is null");
            return;
        }

        // Save the speed and pitch to shared preferences in the app
        SharedPreferences sharedPref = context.get().getSharedPreferences(context.get().getString(R.string.sp_shared_preferences), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(context.get().getString(R.string.sp_audio_speed), speed);
        editor.putFloat(context.get().getString(R.string.sp_audio_pitch), pitch);
        editor.apply(); // Here we can choose between commit() that waits for the write to be done or apply() that does the writing in background

        //String text = String.format("Playback params saved to speed: %.1f pitch: %.1f\n", speed, pitch);
        //LogDAO.getInstance().add(text);
    }

    public void playVoiceMessage(int voiceID, boolean pauseAfterPlayingMessage)
    {
        if(!isEnabled())
            return;

        setShouldPauseAfterCompletion(pauseAfterPlayingMessage);
        voicePlaying = true;
        play(FileManager.getInstance().getVoiceUri(voiceID));
    }

    public boolean isPlaying()
    {
        if(mediaPlayer != null && mediaPlayer.isPlaying())
            return true;

        return false;
    }

    public void togglePlayPause()
    {
        if(!isEnabled())
            return;

        if (mediaPlayer != null)
        {
            if (mediaPlayer.isPlaying() )
            {
                // Save the current position
                setCurrentPosition(mediaPlayer.getCurrentPosition());

                // Play the "Paused" voice
                playVoiceMessage(VoiceID.VID_PAUSING.ordinal(), true);

                // The actual pause code will be called in MediaPlayerManager::onCompletion()
            }
            else
            {
                // Go to the correct position in current file
                playVoiceMessage(VoiceID.VID_CONTINUING.ordinal(), false);
            }
        }
        else
        {
            // This will happen every time after "Stop" is pressed
            // Start a new player
            // Play "continuing" voice
            // After that the playing the newspaper will start automatically
            playVoiceMessage(VoiceID.VID_CONTINUING.ordinal(), false);
        }
    }
/*
    // Did work with one distributor, save the code for later use
    public void playNextNewspaper()
    {
        if(!isEnabled())
            return;

        // Always start from beginning of file when changing newspaper
        resetCurrentPosition();

        // Change folder, this will not start playing the media
        if(!FileManager.getInstance().gotoNextPlayableFolder())
        {
            FileManager.getInstance().gotoFirstPlayableFolder();
        }

        // Play the voice of changing to next Newspaper
        playVoiceMessage(VoiceID.VID_NEXT_NEWSPAPER.ordinal(), false);
    }
*/
    public void playPreviousNewspaper()
    {
        if(!isEnabled())
            return;

        // Always start from beginning of file when changing newspaper
        resetCurrentPosition();

        // Change folder, this will not start playing the media
        if(!FileManager.getInstance().gotoPreviousPlayableFolder())
        {
            FileManager.getInstance().gotoFirstPlayableNewspaperInCurrentDate();
        }

        // Play the voice of changing to previous Newspaper
        playVoiceMessage(VoiceID.VID_PREVIOUS_NEWSPAPER.ordinal(), false);
    }

    public void setSpeed(float speedValue, boolean enableOverride)
    {
        audio_speed = speedValue;

        setPlaybackParamsToMedia(mediaPlayer, audio_speed, audio_pitch);
        savePlaybackParams(audio_speed, audio_pitch);
    }

    public void playNextArticle(boolean pauseAfterVoiceCompletion)
    {
        if(!isEnabled())
            return;

        // Always start from beginning of file when changing article
        resetCurrentPosition();

        FileManager.getInstance().gotoNextPlayableFile();

        // Play the voice of changing to next article
        playVoiceMessage(VoiceID.VID_NEXT_ARTICLE.ordinal(), pauseAfterVoiceCompletion);
    }

    public void playPreviousArticle(boolean pauseAfterVoiceCompletion)
    {
        if(!isEnabled())
            return;

        // Always start from beginning of file when changing article
        resetCurrentPosition();

        FileManager.getInstance().gotoPreviousPlayableFile();

        // Play the voice of changing to previous article
        playVoiceMessage(VoiceID.VID_PREVIOUS_ARTICLE.ordinal(), pauseAfterVoiceCompletion);
    }

    public boolean stop()
    {
        // Stop and release the old media if we are playing
        try
        {
            if (mediaPlayer != null)
            {
                if (mediaPlayer.isPlaying())
                    mediaPlayer.stop();

                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
                return true;
            }
            else {
                //LogDAO.getInstance().add("Mediaplayer is null");
                return false;
            }
        }
        catch (IllegalStateException e)
        {
            LogDAO.getInstance().add(e.toString());
            return false;
        }
    }
}
