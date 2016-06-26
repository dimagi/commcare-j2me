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

package org.javarosa.formmanager.api;

import org.javarosa.core.api.State;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.TrivialTransitions;
import org.javarosa.formmanager.view.ProgressScreen;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.services.transport.TransportListener;
import org.javarosa.services.transport.TransportMessage;
import org.javarosa.services.transport.TransportService;
import org.javarosa.services.transport.impl.TransportException;
import org.javarosa.services.transport.impl.simplehttp.SimpleHttpTransportMessage;
import org.javarosa.services.transport.senders.SenderThread;
import org.javarosa.xform.util.XFormUtils;

import java.io.ByteArrayInputStream;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

public abstract class GetFormHttpState implements State,TrivialTransitions,HandledCommandListener,TransportListener {

    protected ProgressScreen progressScreen;

    private ByteArrayInputStream bin;

    private SenderThread sendThread;


    public GetFormHttpState() {

    }

    public abstract String getURL();

    public void fetchForm(){
        SimpleHttpTransportMessage message = new SimpleHttpTransportMessage(getURL());

        try {
            sendThread = TransportService.send(message);
            sendThread.addListener(this);
        } catch (TransportException e) {
            //TODO: Isn't there a screen where this can be displayed?
            fail("Transport Error while downloading form!" + e.getMessage());
        }
    }

    public void start() {
        this.progressScreen = initProgressScreen();
        J2MEDisplay.setView(progressScreen);
        fetchForm();
    }

    protected ProgressScreen initProgressScreen() {
        return new ProgressScreen("Downloadng","Please Wait. Fetching Form...", this);
    }

    public void fail(String message) {
        progressScreen.setText(message);
        progressScreen.addCommand(progressScreen.CMD_RETRY);
    }

    public void commandAction(Command c, Displayable d) {
        CrashHandler.commandAction(this, c, d);
    }

    public void _commandAction(Command command, Displayable display) {
        if(display == progressScreen){
            if(command==progressScreen.CMD_CANCEL){
                sendThread.cancel();
                done();
            } if(command == progressScreen.CMD_RETRY) {
                start();
            }
        }
    }

    public void process(byte[] response) {
        IStorageUtility formStorage = StorageManager.getStorage(FormDef.STORAGE_KEY);

        bin = new ByteArrayInputStream(response);
        formStorage.write(XFormUtils.getFormFromInputStream(bin));
        done();
    }

    public void onChange(TransportMessage message, String remark) {
        progressScreen.setText(remark);
    }

    public void onStatusChange(TransportMessage message) {
        SimpleHttpTransportMessage httpMessage = (SimpleHttpTransportMessage)message;
        if(httpMessage.isSuccess()) {
            process(httpMessage.getResponseBody());
        } else {
            fail("Failure while fetching XForm: " + message.getFailureReason());
        }
    }

    public abstract void done();
}
