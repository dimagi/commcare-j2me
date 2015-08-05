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
 *
 */
package org.javarosa.communication.reporting.activity;

import org.javarosa.communication.reporting.properties.FeedbackReportProperties;
import org.javarosa.communication.reporting.view.FeedbackReportScreen;
import org.javarosa.core.api.State;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.util.TrivialTransitions;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.view.J2MEDisplay;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

/**
 * @author Clayton Sims
 * @date Feb 27, 2009
 *
 */
public abstract class FeedbackReportState implements TrivialTransitions, State, HandledCommandListener {

    FeedbackReportScreen screen;

    public void start() {
        screen = new FeedbackReportScreen("");
        screen.setCommandListener(this);
        J2MEDisplay.setView(screen);
    }

    public void commandAction(Command c, Displayable d) {
        CrashHandler.commandAction(this, c, d);
    }

    public void _commandAction(Command c, Displayable d) {
        if(c.equals(FeedbackReportScreen.SEND_REPORT)) {
            String message = screen.getString();

            String url = PropertyManager._().getSingularProperty(FeedbackReportProperties.FEEDBACK_REPORT_SERVER);

            String id = PropertyManager._().getSingularProperty("DeviceID");

            throw new RuntimeException("Feedback Reporting: needs to be converted to new transport layer");
//            //TODO: For now, only http is supported, so we hack this to switch to that.
//            int httpmethod = (new HttpTransportMethod()).getId();
//            TransportManager._().setCurrentTransportMethod(httpmethod);
//
//            TransportMessage tmessage = new TransportMessage(new ByteArrayPayload(message.getBytes(), "Feedback Message",IDataPayload.PAYLOAD_TYPE_TEXT), new HttpTransportDestination(url),id,-1);
//            TransportManager._().send(tmessage, httpmethod);
//
//            //TODO: Feedback!
//
//            done();
        } else if (c.equals(FeedbackReportScreen.CANCEL)) {
            done();
        }
    }

}
