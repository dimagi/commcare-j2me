/*
 * Copyright (C) 2009 JavaRosa
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * An Activity that represents the capture of audio.
 *
 * @author Ndubisi Onuora
 *
 */

package org.javarosa.media.audio.activity;

import org.javarosa.core.api.State;
import org.javarosa.core.services.UnavailableServiceException;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.log.HandledThread;
import org.javarosa.j2me.services.AudioCaptureService;
import org.javarosa.j2me.services.DataCaptureServiceRegistry;
import org.javarosa.j2me.services.exception.AudioException;
import org.javarosa.j2me.services.exception.FileException;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.media.image.activity.DataCaptureTransitions;
import org.javarosa.media.image.model.FileDataPointer;

import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.midlet.MIDlet;

public abstract class AudioCaptureState implements DataCaptureTransitions, State, HandledCommandListener, Runnable
{
    //private final long FOREVER = 1000000;
    private AudioCaptureService recordService;

    private Command recordCommand, playCommand, stopCommand, backCommand,
                    saveCommand, eraseCommand, finishCommand;
    private OutputStream audioDataStream;
    private String fullName, audioFileName;
    private RecorderForm form;
    private FileDataPointer recordFile;

    private StringItem messageItem;
    private StringItem errorItem;

    Thread captureThread;
    boolean captureThreadStarted = false;

   // private static int counter = 0; //Used for saving files

    MIDlet recMid;
    DataCaptureServiceRegistry dc;
    DataCaptureTransitions transitions;

    public AudioCaptureState (MIDlet m, DataCaptureServiceRegistry dc)
    {
        recMid = m;
        this.dc = dc;
        transitions = this;
    }

    //Finish off construction of Activity
    public void start()
    {
        form = new RecorderForm();

        messageItem = form.getMessageItem();
        errorItem = form.getErrorItem();

        initCommands();
        audioFileName = null;
        J2MEDisplay.setView(form);
        form.setCommandListener(this);

        try
        {
            recordService = dc.getAudioCaptureService();
        }
        catch(UnavailableServiceException ue)
        {
            serviceUnavailable(ue);
        }
        captureThread = new HandledThread(this, "CaptureThread");
    }

    public void destroy()
    {
        try
        {
            if(recordService != null)
                recordService.closeStreams();
        }
        catch(IOException ioe)
        {
            System.err.println("An error occurred while closing the streams of the AudioCaptureService.");
            ioe.printStackTrace();
        }
    }

    private void initCommands()
    {
        recordCommand = new Command("Record", Command.SCREEN, 0);
        form.addCommand(recordCommand);
        playCommand = new Command("Play", Command.SCREEN, 1);
        stopCommand = new Command("Stop", Command.SCREEN, 0); //Do not add immediately
        backCommand = new Command("Back", Command.BACK, 0);
        form.addCommand(backCommand);
        finishCommand = new Command("Finish", Command.OK, 0);
        saveCommand = new Command("Save", Command.SCREEN, 0);
        eraseCommand = new Command("Erase", Command.SCREEN, 0);
    }

    public void commandAction(Command c, Displayable d) {
        CrashHandler.commandAction(this, c, d);
    }

    public void _commandAction(Command comm, Displayable disp) {
        //Record to file
        if(comm == recordCommand)
        {
            captureThread.setPriority(Thread.currentThread().getPriority() +1 );
            if(!captureThreadStarted)
            {
                captureThread.start();
                captureThreadStarted = true;
            }
            else
                captureThread.run();
        }
        //User should be able to replay recording
        else if(comm == playCommand)
        {
            try
            {
                playAudio();
            }
            catch(AudioException ae)
            {
                errorItem.setLabel("Error playing audio");
                System.err.println(ae.toString());
            }
            catch(FileException fe)
            {
                errorItem.setLabel("Error playing audio");
                System.err.println(fe.toString());
            }
        }
        //Stop recording or playing audio
        else if(comm == stopCommand)
        {
            try
            {
                stop();
            }
            catch(AudioException ae)
            {
                errorItem.setLabel("Error stopping action");
                //errorItem.setText(ae.toString());
                System.err.println(ae.toString());
            }
        }
        else if(comm == backCommand)
        {
            moveBack();
        }
        else if(comm == finishCommand)
        {
            finalizeTask();
        }
        else if(comm == saveCommand)
        {
            //String fileName = "Audio" + counter + ".wav";
            if(audioDataStream == null)
                audioDataStream = recordService.getAudio();
            saveFile(audioFileName);
        }
        else if(comm == eraseCommand)
        {
            removeFile();
        }
    }

    public void recordAudio() throws AudioException, FileException, InterruptedException
    {
          errorItem.setLabel("");
          errorItem.setText("");

          recordService.startRecord();

          messageItem.setText("Recording...");

          form.removeCommand(recordCommand); //"Hide" recordCommand when recording has stopped
          form.addCommand(stopCommand);
          form.addCommand(saveCommand);
    }

      public void playAudio() throws AudioException, FileException
      {
          errorItem.setLabel("");
          errorItem.setText("");
          System.err.println("Attempting to play audio...");
          messageItem.setText("Starting the Player...");

          recordService.startPlayback();

          try
          {
              if(recMid!= null)
                  recMid.platformRequest(fullName);
              else
                  throw new ConnectionNotFoundException("Since midlet is null, try 2nd method of playback");
          }
          catch(ConnectionNotFoundException cnfe)
          {
              cnfe.printStackTrace();
              /*If the platform request fails, which it shouldn't, attempt to start playback
               * through the service.*/
                recordService.startPlayback();
          }

          System.err.println("Player has started.");
          messageItem.setText("Player has started!");

          form.removeCommand(eraseCommand);
          form.removeCommand(saveCommand);
          form.removeCommand(playCommand); //"Hide" playCommand when playing has started
          form.removeCommand(recordCommand); //"Hide" recordCommand when playing has started
          form.addCommand(stopCommand); //Show recordCommand when playing has started

          //Thread.currentThread().sleep(FOREVER);
      }

      //General method to stop recording or playback
      public void stop() throws AudioException
      {
          //If currently recording
          if(recordService.getState() == AudioCaptureService.CAPTURE_STARTED)
          {
              stopCapturing();
          }
          else if(recordService.getState() == AudioCaptureService.PLAYBACK_STARTED)
          {
              stopPlaying();
          }
      }

      public void stopCapturing() throws AudioException
      {
          errorItem.setLabel("");
          errorItem.setText("");
          System.err.println("Attempting to stop recording");

          /*
          captureThread.interrupt();
          captureThread.setPriority(Thread.currentThread().getPriority() -1 );
          */

          recordService.stopRecord();

          form.removeCommand(stopCommand); //"Hide" stopCommand when recording desires to resume
          form.addCommand(recordCommand);
          form.addCommand(playCommand);
          form.addCommand(eraseCommand);
          form.addCommand(finishCommand);

          messageItem.setText("Stopping the Recorder...");
          audioDataStream = recordService.getAudio();

          //System.err.println("Sound size=" + audioDataStream.toByteArray().length);

          messageItem.setText("Stopped Recording!");
      }

      //Stops the playback of the Recorder
      public void stopPlaying() throws AudioException
      {
          errorItem.setLabel("");
          errorItem.setText("");
          //Application must wait for Player to finish playing
          recordService.stopPlayback();
          System.err.println("Player has been stopped.");
          messageItem.setText("Stopped Playing!");

          form.removeCommand(stopCommand);
          form.addCommand(playCommand);
          form.addCommand(recordCommand);
          form.addCommand(saveCommand);
          form.addCommand(eraseCommand);
      }

      public FileDataPointer getRecordedAudio()
      {
          if(recordFile == null)
              recordFile = new FileDataPointer(fullName);
          return recordFile;
      }

      /*
      private void readFile(String fileName)
      {
          try
          {
              String rootName = fileService.getDefaultRoot();
              String restorepath = "file:///" + rootName + "JRSounds";
              String fullName = restorepath + "/" + fileName;

              recordFile = new FileDataPointer(fullName);
              System.err.println("Successfully read Music file");

              //System.out.println("Sound Size =" + recordFile.getData().length);

              finalizeTask();
          }
          catch(FileException fe)
          {
              System.err.println(fe);
              fe.printStackTrace();
          }
      }
      */

      private String saveFile(String filename)
      {
          errorItem.setLabel("");
          errorItem.setText("");
          try
          {
              /*
               * Stop capturing if the save command is activated without a subsequent activation of the
               * stop command. Recorder player must be started but not closed.
               */

              if(recordService.getState() == AudioCaptureService.CAPTURE_STARTED && recordService.getState() != AudioCaptureService.CLOSED)
              {
                  stopCapturing();
              }
          }
          catch(AudioException ae)
          {
              System.err.println("An error occured when attempting to stop audio capture!");
          }

          try
          {
              recordService.saveRecording(audioFileName);
              fullName = recordService.getAudioPath();
          }
          catch(FileException fe)
          {
              errorItem.setText("Error saving audio.");
              System.err.println(fe);
              fe.printStackTrace();

              return "";
          }

          System.out.println("Sound saved to:" + fullName);
          messageItem.setText("Saved to:" + fullName);
          recordFile = new FileDataPointer(fullName);
          form.addCommand(eraseCommand);

          return fullName;
      }

      //Removes the captured audio
      public void removeFile()
      {
          errorItem.setLabel("");
          errorItem.setText("");

          boolean eraseSucceeded = false;

          try
          {
              recordService.removeRecording();
              eraseSucceeded = true;
          }
          catch(FileException fe)
          {
              eraseSucceeded = false;
              messageItem.setText("");
              errorItem.setText("Error erasing audio");
              fe.printStackTrace();
              System.err.println("Error erasing captured audio!");
          }
          if(eraseSucceeded)
          {
              messageItem.setText("Erased audio at " + fullName);
              recordFile = null;
              form.removeCommand(eraseCommand);
              form.removeCommand(saveCommand);
              form.removeCommand(playCommand);
          }
      }

      //Go back one screen
      public void moveBack()
      {
          System.err.println("Moving back");
          destroy();
          transitions.cancel();
      }

      //Finish capturing audio, inform shell, and return data as well
      public void finalizeTask()
      {
          System.err.println("Finalizing audio capture");

          destroy();
          if (recordFile != null) {
              transitions.captured(recordFile);
          } else {
              transitions.noCapture();
          }
      }

      //Actions to perform when service is unavailable
      private void serviceUnavailable(Exception e)
      {
          errorItem.setText("The Audio Capture or File Service is unavailable.\n QUITTING!");

          //The only thing the user can do is retreat
          form.removeCommand(recordCommand);
          form.removeCommand(playCommand);

          System.err.println(e.getMessage());
      }

      //Record audio in a separate thread to keep the command listener alert for stopping
      public void run()
      {
            try
            {
              recordAudio();
            }
            catch(AudioException ae)
            {
                errorItem.setLabel("Error recording audio");
                //errorItem.setText(ae.toString());
                System.err.println(ae.toString());
            }
            catch(FileException fe)
            {
                errorItem.setLabel("Error recording audio");
                //errorItem.setText(ae.toString());
                System.err.println(fe.toString());
            }
            catch(InterruptedException ie)
            {
                errorItem.setLabel("Error occurred while recording");
                //errorItem.setText(ie.toString());
                System.err.println(ie.toString());
            }
      }
}

class RecorderForm extends Form
{
    private StringItem messageItem;
    private StringItem errorItem;

    public RecorderForm()
    {
        super("Record Audio");
        messageItem = new StringItem("", "Press Record to start recording.");
        append(messageItem);
        errorItem = new StringItem("", "");
        append(errorItem);
        //StringBuffer inhalt = new StringBuffer();
    }

    public StringItem getMessageItem()
    {
        return messageItem;
    }

    public StringItem getErrorItem()
    {
        return errorItem;
    }

}