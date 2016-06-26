package org.javarosa.formmanager.api;

import org.javarosa.core.api.State;
import org.javarosa.formmanager.api.transitions.CompletedFormOptionsTransitions;

public abstract class CompletedFormOptionsState implements CompletedFormOptionsTransitions, State {

    protected String messageId;

    public CompletedFormOptionsState (String messageId) {
        this.messageId = messageId;
    }

    public void start () {
        CompletedFormOptionsController controller = getController();
        controller.setTransitions(this);
        controller.start();
    }

    protected CompletedFormOptionsController getController () {
        return new CompletedFormOptionsController(messageId);
    }

}
