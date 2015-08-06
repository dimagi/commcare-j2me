/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.javarosa.media.image.activity;

import org.javarosa.core.api.State;
import org.javarosa.core.services.UnavailableServiceException;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.log.HandledThread;
import org.javarosa.j2me.services.FileService;
import org.javarosa.j2me.services.exception.FileException;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.media.image.model.FileDataPointer;
import org.javarosa.utilities.file.J2MEFileService;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

/**
 * The <code>FileBrowser</code> custom component lets the user list files and
 * directories. It's uses FileConnection Optional Package (JSR 75). The FileConnection
 * Optional Package APIs give J2ME devices access to file systems residing on mobile devices,
 * primarily access to removable storage media such as external memory cards.
 * This code has been wrapped in an activity by Cory Zue to work in the JavaRosa framework
 * @author breh
 * @author Cory Zue
 */

public abstract class FileBrowseState implements DataCaptureTransitions, State, HandledCommandListener {

    private String currDirName;

    private Command selectCommand = new Command("Select", Command.OK, 1);
    private Command returnCommand = new Command("Return Directory", Command.OK, 2);
    private Command back = new Command("Back", Command.BACK, 2);
    private Command cancel = new Command("Cancel", Command.CANCEL, 4);

    private FileService fileService;
    private DataCaptureTransitions transitions;

    private final static String UP_DIRECTORY = "/";
    private final static String MEGA_ROOT = "/";
    private final static String SEP_STR = "/";
    private final static char SEP = '/';

    private int mode;

    public static final int MODE_FILE = 0;
    public static final int MODE_DIRECTORY = 1;


    public FileBrowseState() {
        this(MODE_FILE);
    }

    public FileBrowseState (int mode)
    {
        transitions = this;
        setMode(mode);
        //this.currDirName = FileUtility.getDefaultRoot();
        try
        {
            fileService = getFileService();
            this.currDirName = fileService.getDefaultRoot();
        }
        catch(UnavailableServiceException ue)
        {
            serviceUnavailable(ue);
        }
        catch(FileException fe)
        {
            fe.printStackTrace();
        }

    }

    public void start() {
    //    boolean isAPIAvailable = false;
        if (System.getProperty("microedition.io.file.FileConnection.version") != null) {
            //isAPIAvailable = true;
            try
            {
                showCurrDir();
            }
            catch (SecurityException e)
            {
                System.out.println(e);
            }
            catch (Exception e)
            {
                System.out.println(e);
            }
        } else {
            String splashText = new String("Sorry - not available");
            Alert splashScreen = new Alert(null, splashText, null,
                    AlertType.INFO);
            splashScreen.setTimeout(3000);
            J2MEDisplay.setView(splashScreen);
        }
    }

    public void commandAction(Command c, Displayable d) {
        CrashHandler.commandAction(this, c, d);
    }

    public void _commandAction(Command c, Displayable d) {
        System.out.println("updir:" + UP_DIRECTORY);
        if (c == selectCommand ) {
            List curr = (List) d;
            final String currFile = curr.getString(curr.getSelectedIndex());
            new HandledThread(new Runnable() {
                public void run() {
                    if (currFile.endsWith(SEP_STR)
                            || currFile.equals(UP_DIRECTORY)) {
                        traverseDirectory(currFile);
                    } else {
                        returnFile(currFile);
                    }
                }
            }).start();
        } else if (c == returnCommand) {
            List curr = (List)d;
            returnDirectory(curr.getString(curr.getSelectedIndex()));
        } else if (c == back)
        {
            try
            {
                showCurrDir();
            }
            catch(FileException fe)
            {
                System.err.println("An FileException occurred while showing the current directory.");
                fe.printStackTrace();
            }
        }
        else if (c == cancel) {
            transitions.cancel();
        }
    }

    /**
     * Sets the mode of this, currently DIRECTORY or FILE
     * @param mode
     */
    public void setMode(int mode) {
        this.mode = mode;
    }

    private void showCurrDir() throws FileException
    {
        String[] strArr;
        List browser;
        System.out.println("In showCurrDir");
        System.out.println("mega_root:" + MEGA_ROOT + "cur_dir:"
                + currDirName);
        if (MEGA_ROOT.equals(currDirName)) {
            strArr = fileService.getRootNames();
            browser = new List(currDirName, List.IMPLICIT);
            System.out.println("here");
        } else {
            String fullPath =  "file://localhost/" + currDirName;
            strArr = fileService.listDirectory(fullPath);
            browser = new List(currDirName, List.IMPLICIT);
            browser.append(UP_DIRECTORY, null);
        }
        for(int i = 0; i < strArr.length; ++i)
        {
            String fileName = strArr[i];
            if (fileName.charAt(fileName.length() - 1) == SEP) {
                // this is a directory
                browser.append(fileName, null);
            }
            // if((fileName.charAt(fileName.length()-1))).equals("g"))){}
            else {
                System.out.println("h4");
                // Image image = Image.createImage(fileName);
                // this is a file
                if (mode == MODE_FILE) {
                    browser.append(fileName, null);
                }
                // Form form = new Form("Image here");

                // form.append(image);
            }
        }
        browser.setSelectCommand(selectCommand );
        if (mode == MODE_DIRECTORY) {
            browser.addCommand(returnCommand);
        }

        browser.addCommand(cancel);
        browser.setCommandListener(this);
        J2MEDisplay.setView(browser);
    }

    private void traverseDirectory(String fileName) {
        System.out.println("fileName:" + fileName + "cur_dir:" + currDirName
                + "mega_root:" + MEGA_ROOT);
        if (currDirName.equals(MEGA_ROOT)) {
            if (fileName.equals(UP_DIRECTORY)) {
                // can not go up from MEGA_ROOT
                return;
            }
            currDirName = fileName;
        } else if (fileName.equals(UP_DIRECTORY)) {
            System.out.println("up");
            // Go up one directory
            // TODO use setFileConnection when implemented
            int i = currDirName.lastIndexOf(SEP, currDirName.length() - 2);
            if (i != -1) {
                currDirName = currDirName.substring(0, i + 1);
            } else {
                currDirName = MEGA_ROOT;
            }
        } else {
            currDirName = currDirName + fileName;
        }
        try
        {
            showCurrDir();
        }
        catch(FileException fe)
        {
            System.err.println("An FileException occurred while showing the current directory.");
            fe.printStackTrace();
        }
    }

    private void returnFile(String fileName) {
        if (fileName == "/") {
            fileName = "";
        }
        String fullName = "file://localhost/" + currDirName + fileName;
        FileDataPointer fdp = new FileDataPointer(fullName);
        transitions.captured(fdp);
    }

    private void returnDirectory(String name) {
        returnFile(name);
    }

    private void serviceUnavailable(Exception e)
    {
        System.err.println("The File Service is unavailable.\n QUITTING!");
        System.err.println(e.getMessage());
    }

    private FileService getFileService() throws UnavailableServiceException
    {
        //#if app.usefileconnections
        //# return new J2MEFileService();
        //#else
        throw new UnavailableServiceException("Unavailable service: " +  J2MEFileService.serviceName);
        //#endif
    }
}
