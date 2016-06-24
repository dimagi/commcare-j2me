/**
 *
 */
package org.javarosa.formmanager.api;

import org.javarosa.formmanager.api.transitions.CompletedFormOptionsTransitions;
import org.javarosa.formmanager.view.transport.SendNowSendLaterForm;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.log.HandledItemStateListener;
import org.javarosa.j2me.view.J2MEDisplay;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Item;

/**
 * @author ctsims
 *
 */
public class CompletedFormOptionsController implements HandledCommandListener, HandledItemStateListener {
    protected CompletedFormOptionsTransitions transitions;
    protected String messageId;
    protected SendNowSendLaterForm view;

    public CompletedFormOptionsController(String messageId) {
        this(messageId, false);
    }

    public CompletedFormOptionsController(String messageId, boolean cacheAutomatically) {
        this.messageId = messageId;
        view = new SendNowSendLaterForm(this, this, cacheAutomatically);
    }

    public void setTransitions (CompletedFormOptionsTransitions transitions) {
        this.transitions = transitions;
    }

    public void start() {
        J2MEDisplay.setView(view);
    }

    public void commandAction(Command c, Displayable d) {
        CrashHandler.commandAction(this, c, d);
    }

    public void _commandAction(Command c, Displayable d) {
        //If we're just on the acknowledgment screen, we always want
        //to just skip.
        if(c == view.commandOk) {
            transitions.skipSend(messageId);
            return;
        }

        int choice = view.getCommandChoice();
        if(choice == SendNowSendLaterForm.SEND_NOW_DEFAULT) {
            transitions.sendData(messageId);
        } else if(choice == SendNowSendLaterForm.SEND_LATER) {
            transitions.skipSend(messageId);
        }
    }

    public void itemStateChanged(Item i) {
        CrashHandler.itemStateChanged(this, i);
    }

    public void _itemStateChanged(Item i) {
        switch (view.getCommandChoice()) {
        case SendNowSendLaterForm.SEND_NOW_DEFAULT:
            transitions.sendData(messageId);
            break;
        case SendNowSendLaterForm.SEND_LATER:
            //Since caching and sending later are now part of the queue, we need
            //to let the transition have the data now. Possibly revisit this later.
            transitions.skipSend(messageId);
            break;
        }
    }

}
