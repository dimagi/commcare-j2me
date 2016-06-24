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

package org.javarosa.formmanager.view;

import org.javarosa.core.services.locale.Localization;
import org.javarosa.j2me.log.HandledThread;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;

public class ProgressScreen extends Form{
    protected Gauge progressbar;
    protected Thread updater_T;
    public final Command CMD_CANCEL = new Command(Localization.get("polish.command.cancel"),Command.BACK, 1);
    public final Command CMD_RETRY = new Command(Localization.get("menu.retry"),Command.ITEM, 1);

    public ProgressScreen(String title, String msg, CommandListener cmdListener) {
        this(title, msg, cmdListener, false);
    }

    public ProgressScreen(String title, String msg, CommandListener cmdListener, boolean continuous) {
        super(title);
        //#style focused
        progressbar = new Gauge(msg, false, Gauge.INDEFINITE, continuous ? Gauge.CONTINUOUS_RUNNING : 0);

        addCommand(CMD_CANCEL);
        append(progressbar);
        setCommandListener(cmdListener);

        if (!continuous) {
            updater_T = new HandledThread(new GaugeUpdater());
            updater_T.start();
        }
    }

    public void setText(String text) {
        progressbar.setLabel(text);
    }

    public void closeThread(){
        if(updater_T!=null){
            updater_T = null;
        }
    }

    class GaugeUpdater implements Runnable {

        public void run() {

            while (progressbar.getValue() < progressbar.getMaxValue()) {
//                progressbar.setValue(progressbar.getValue() + 1);
                progressbar.setValue(Gauge.CONTINUOUS_IDLE);
            }

        }
    }


}
