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

package org.javarosa.formmanager.view.transport;

import org.javarosa.core.services.locale.Localization;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.Spacer;

import de.enough.polish.ui.Command;
import de.enough.polish.ui.FramedForm;
import de.enough.polish.ui.StringItem;

public class SendNowSendLaterForm extends FramedForm {
    private ChoiceGroup cg;

    private boolean seenKeyPressed = false;

    public Command commandOk = new Command(Localization.get("polish.command.ok"), Command.OK, 0);

    public static final int SEND_NOW_DEFAULT = 0;
    public static final int SEND_LATER = 1;
    public static final int SEND_NOW_SPEC = 2;

    public SendNowSendLaterForm(CommandListener activity, ItemStateListener itemListener) {
        this(activity, itemListener, false);
    }

    public SendNowSendLaterForm(CommandListener activity, ItemStateListener itemListener, boolean cacheAutomatically) {
        //#style submitPopup
        super(cacheAutomatically ? Localization.get("sending.view.done.title") : Localization.get("sending.view.submit"));

        setCommandListener(activity);

        if(!cacheAutomatically) {
            //#style submitYesNo
            this.cg = new ChoiceGroup(Localization.get("sending.view.when"), Choice.EXCLUSIVE);

            setItemStateListener(itemListener);

            // NOTE! These Indexes are optimized to be added in a certain
            // order. _DO NOT_ change it without updating the static values
            // for their numerical order.
            this.cg.append(Localization.get("sending.view.now"), null);
            this.cg.append(Localization.get("sending.view.later"), null);
            append(this.cg);
        } else {
            //#style submitText
            StringItem message = new StringItem(null, Localization.get("sending.view.done"));
            this.append(message);

            this.addCommand(commandOk);
        }


        //TODO: Add this back in for admin users. Who took it out?
        //this.cg.append(Locale.get("sending.view.new"), null);// clients wont need to
        // see
        // this


        append(new Spacer(80, 0));
    }

    public int getCommandChoice() {
        return this.cg.getSelectedIndex();
    }

    // fix bug in polish 2.1.0 (and possibly earlier) where keydown event answers the last question
    // in the form, and the keyup event from the same button press is passed to this view, automatically
    // selecting 'send now'
    //
    //no exception handling needed
    public void keyPressed(int keyCode) {
        super.keyPressed(keyCode);
        seenKeyPressed = true;
    }
    //
    //no exception handling needed
    public void keyReleased(int keyCode) {
        if (seenKeyPressed) {
            super.keyReleased(keyCode);
        }
    }
    //////
}
