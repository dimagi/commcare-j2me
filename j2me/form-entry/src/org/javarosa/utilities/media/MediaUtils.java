package org.javarosa.utilities.media;

import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.Logger;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.formmanager.properties.FormManagerProperties;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.media.Control;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;

public class MediaUtils {

    /////////AUDIO PLAYBACK

    private final static Object audioLock = new Object();


    private static boolean isPaused = false;
    private static String currentTag = null;

    //We only really every want to play audio through one centralized player, so we'll keep a static
    //instance
    private static Player audioPlayer;

    public static final int AUDIO_SUCCESS = 1;
    public static final int AUDIO_NO_RESOURCE = 2;
    public static final int AUDIO_ERROR = 3;
    public static final int AUDIO_DISABLED = 4;
    public static final int AUDIO_BUSY = 5;
    public static final int AUDIO_NOT_RECOGNIZED = 6;

    public static int playOrPauseAudio(String jrRefURI, String tag) {
        if(tag != null) {
            if(attemptToPauseOrUnpauseAudio(tag)) {
                return AUDIO_SUCCESS;
            }
        }
        int ret = playAudio(jrRefURI);
        if(ret == AUDIO_SUCCESS) {
            currentTag = tag;
        }
        return ret;
    }

    public static boolean audioTurnedOn(){
        String playAudio = PropertyManager._().getSingularProperty(FormManagerProperties.PLAY_AUDIO);
        return (!FormManagerProperties.PLAY_AUDIO_NO.equals(playAudio));
    }

    /**
     * Begin audio playback of the content at the uri.
     *
     * @param jrRefURI a JR reference to audio content
     * @param tag an optional tag to associate with this playback to
     * allow it to be paused/resumed.
     * @return One of the AUDIO_ return codes
     */
        public static int playAudio(String jrRefURI) {

            if(!audioTurnedOn()){
                return AUDIO_DISABLED;
            }

            synchronized(audioLock) {
                String curAudioURI = jrRefURI;
                int retcode = AUDIO_SUCCESS;
                try {
                    Reference curAudRef = ReferenceManager._().DeriveReference(curAudioURI);
                    String format = getFileFormat(curAudioURI);

                    if(format == null) return AUDIO_NOT_RECOGNIZED;

                    //So there's an unfortunate issue where media which isn't associated (and won't play)
                    //can result in audio being stopped when alternate selections are made, but we won't
                    //worry about that for now
                    stopAudio();
                    audioPlayer = MediaUtils.getPlayerLoose(curAudRef);

                    //Sometimes there's just no audio player
                    if(audioPlayer == null) {
                        return AUDIO_DISABLED;
                    }
                    audioPlayer.realize();
                    crankAudio(audioPlayer);
                    audioPlayer.start();
                } catch (InvalidReferenceException ire) {
                    retcode = AUDIO_ERROR;
                    System.err.println("Invalid Reference Exception when attempting to play audio at URI:"+ curAudioURI + "Exception msg:"+ire.getMessage());
                } catch (IOException ioe) {
                    retcode = AUDIO_ERROR;
                    System.err.println("IO Exception (input cannot be read) when attempting to play audio stream with URI:"+ curAudioURI + "Exception msg:"+ioe.getMessage());
                } catch (MediaException e) {
                    //TODO: We need to figure out how to deal with silent stuff correctly
                    //Logger.log("auderme", e.getMessage());
                    //J2MEDisplay.showError(null, "Phone is on silent!");

                    retcode = AUDIO_ERROR;
                    System.err.println("Media format not supported! Uri: "+ curAudioURI + "Exception msg:"+e.getMessage());
                } catch(SecurityException e) {
                    //Logger.log("auderse", e.getMessage());
                    //J2MEDisplay.showError(null, "Phone is on silent!");
                }
                return retcode;
            }
        }

        private static String getFileFormat(String fpath){
//            Wave audio files: audio/x-wav
//            AU audio files: audio/basic
//            MP3 audio files: audio/mpeg
//            MIDI files: audio/midi
//            Tone sequences: audio/x-tone-seq
//            MPEG video files: video/mpeg
//            Audio 3GPP files (.3gp) audio/3gpp
//            Audio AMR files (.amr) audio/amr
//            Audio AMR (wideband) files (.awb) audio/amr-wb
//            Audio MIDI files (.mid or .midi) audio/midi
//            Audio MP3 files (.mp3) audio/mpeg
//            Audio MP4 files (.mp4) audio/mp4
//            Audio WAV files (.wav) audio/wav audio/x-wav

            if(fpath.indexOf(".mp3") > -1) return "audio/mp3";
            if(fpath.indexOf(".wav") > -1) return "audio/x-wav";
            if(fpath.indexOf(".amr") > -1) return "audio/amr";
            if(fpath.indexOf(".awb") > -1) return "audio/amr-wb";
            if(fpath.indexOf(".mp4") > -1) return "audio/mp4";
            if(fpath.indexOf(".aac") > -1) return "audio/aac";
            if(fpath.indexOf(".3gp") > -1) return "audio/3gpp";
            if(fpath.indexOf(".au") > -1) return "audio/basic";
            throw new RuntimeException("COULDN'T FIND FILE FORMAT");
        }

        /**
         * Log that a media file was started, paused, or stopped (i.e., finished playing).
         *
         * TODO: move this to some sort of 'form entry diagnostics' framework instead of bloating the logs.
         * @param event From PlayerListener
         * @param uri Media file location
         */
        public static void logEvent(String event, String uri) {
            String action = "";
            if (event == PlayerListener.STARTED) {
                action = "start";
            }
            if (event == PlayerListener.STOPPED) {
                action = "pause";
            }
            else if (event == PlayerListener.END_OF_MEDIA) {
                action = "stop";
            }
            if (!"".equals(action)) {
                try {
                    Vector<String> pieces = DataUtil.split(uri, "/", false);
                    uri = pieces.lastElement();
                } catch (Exception e) {
                    // just use the full URI
                }
                Logger.log("media", action + ": " + uri);
            }
        }

        public static Player getPlayerLoose(Reference reference) throws MediaException, IOException {
            Player thePlayer;

            try{
                thePlayer = Manager.createPlayer(reference.getLocalURI());
                final String uri = reference.getLocalURI();
                thePlayer.addPlayerListener(new PlayerListener() {
                    public void playerUpdate(Player player, String event, Object eventData) {
                        logEvent(event, uri);
                    }
                });
                return thePlayer;
            } catch(MediaException e) {
                if(!FormManagerProperties.LOOSE_MEDIA_YES.equals(PropertyManager._().getSingularProperty(FormManagerProperties.LOOSE_MEDIA))) {
                    throw e;
                }
                Reference[] refs = reference.probeAlternativeReferences();
                for(Reference ref : refs) {
                    if(ref.doesBinaryExist()) {
                        try{
                            //TODO: Make sure you create a player of the right type somehow (video/audio), don't want
                            //to accidentally send back an audio player of a video file
                            thePlayer = Manager.createPlayer(ref.getLocalURI());
                            return thePlayer;
                        }catch(MediaException oe) {
                            //also bad file, keep trying
                        }
                    }
                }
                throw e;
            }
        }


        public static Player crankAudio(Player thePlayer) {
            if(thePlayer == null) { return thePlayer; }
            for(Control control : thePlayer.getControls()) {
                //Set up our player if we can, depending on what controls are available;
                if(control instanceof VolumeControl) {
                    VolumeControl vc = (VolumeControl)control;
                    vc.setLevel(100);
                }
            }
            return thePlayer;
        }


        /**
         * Stops playback of any audio currently playing, and deallocates
         * any associated players or state associated with the current
         * audio.
         */
        public static void stopAudio() {
            synchronized(audioLock) {
                currentTag = null;
                isPaused = false;
                if(audioPlayer != null){
                    try {
                        audioPlayer.stop();
                    } catch (MediaException e) {
                        e.printStackTrace();
                    }
                    audioPlayer.deallocate();
                    audioPlayer.close();
                }
                audioPlayer = null;
            }
        }

        /**
         * If there's currently an audio player playing associated with the tag,
         * this will pause that audio. If audio is currently paused, playback will
         * be resumed.
         *
         * If audio hasn't started, is already finished, isn't associated with the tag,
         * or otherwise isn't available to manipulate this method will do nothing and
         * return false.
         *
         * Paused audio will be completely removed/destroyed if there is a request
         * to play new audio or stop the current audio playback.
         *
         * @param tag A string to associate specific audio playback. Cannot be
         * null.
         *
         * @return
         */
        public static boolean attemptToPauseOrUnpauseAudio(String tag) {
            synchronized(audioLock) {
                if(audioPlayer == null){
                    return false;
                }

                //Make sure we're trying to pause or unpause the same thing as is
                //currently paused
                if(!tag.equals(currentTag)) {
                    //if not, there's nothing we can do
                    return false;
                }

                if(isPaused) {
                    try {
                        //No matter what, clear the flag
                        isPaused = false;

                        //double check we're in the right state
                        if(audioPlayer.getState() == Player.PREFETCHED) {
                            //restart playback
                            audioPlayer.start();
                            return true;
                        } else {
                            //something weird happened and the player isn't in the right place
                            //anymore. Just bail
                            return false;
                        }
                    } catch (MediaException e) {
                        //... weird. Something very bad happened here.
                        e.printStackTrace();
                        //We didn't do anything, so return false
                        return false;
                    }
                } else {
                    if(audioPlayer.getState() == Player.STARTED) {
                        try {
                            audioPlayer.stop();
                            //Unfortunately, there's no way to know here whether we actually
                            //paused or just stopped playback, since it just ignores
                            //the request it finished in the tiny period between getstate
                            //and stop, but just go with it (I think the behavior should look
                            //most the same either way)
                            isPaused = true;
                            return true;
                        } catch (MediaException e) {
                            e.printStackTrace();
                            //We boned something here. Clear out the whole state
                            stopAudio();
                            return false;
                        }
                    }
                }
                //Unless we explicitly returned false above, we didn't handle this
                return false;
            }
        }

}
