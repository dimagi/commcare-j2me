package org.javarosa.utilities.media;

import java.io.IOException;

import javax.microedition.media.Control;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;

import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.formmanager.properties.FormManagerProperties;

public class MediaUtils {
	
    /////////AUDIO PLAYBACK
	
	private final static Object audioLock = new Object();
	
	//We only really every want to play audio through one centralized player, so we'll keep a static
	//instance
	private static Player audioPlayer;
	
	public static final int AUDIO_SUCCESS = 1;
	public static final int AUDIO_NO_RESOURCE = 2;
	public static final int AUDIO_ERROR = 3;
	public static final int AUDIO_DISABLED = 4;
	public static final int AUDIO_BUSY = 5;
	public static final int AUDIO_NOT_RECOGNIZED = 6;
	  
		public static int playAudio(String jrRefURI) {
			synchronized(audioLock) {
				String curAudioURI = jrRefURI;
				int retcode = AUDIO_SUCCESS;
				try {
					Reference curAudRef = ReferenceManager._().DeriveReference(curAudioURI);
					String format = getFileFormat(curAudioURI);
	
					if(format == null) return AUDIO_NOT_RECOGNIZED;
					if(audioPlayer != null){
						audioPlayer.deallocate();
						audioPlayer.close();
					}
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
//			Wave audio files: audio/x-wav
//			AU audio files: audio/basic
//			MP3 audio files: audio/mpeg
//			MIDI files: audio/midi
//			Tone sequences: audio/x-tone-seq
//			MPEG video files: video/mpeg
//			Audio 3GPP files (.3gp) audio/3gpp
//			Audio AMR files (.amr) audio/amr
//			Audio AMR (wideband) files (.awb) audio/amr-wb
//			Audio MIDI files (.mid or .midi) audio/midi
//			Audio MP3 files (.mp3) audio/mpeg
//			Audio MP4 files (.mp4) audio/mp4
//			Audio WAV files (.wav) audio/wav audio/x-wav
			
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

		
		public static Player getPlayerLoose(Reference reference) throws MediaException, IOException {
			Player thePlayer;
			
	        try{ 
	        	thePlayer = Manager.createPlayer(reference.getLocalURI());
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


		public static void stopAudio() {
			synchronized(audioLock) {
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

}
