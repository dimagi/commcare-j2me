/**
 *
 */
package org.javarosa.user.api;

import org.javarosa.core.services.locale.Localization;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.user.api.transitions.EditUserTransitions;
import org.javarosa.core.model.User;
import org.javarosa.user.utility.IUserDecorator;
import org.javarosa.user.utility.UserValidator;
import org.javarosa.user.view.UserForm;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

/**
 * @author ctsims
 *
 */
public class EditUserController implements HandledCommandListener {

    EditUserTransitions transitions;

    UserForm view;

    public final static Command CMD_SAVE = new Command(Localization.get("menu.Save"), Command.OK, 2);
    public final static Command CMD_CANCEL = new Command(Localization.get("menu.Exit"), Command.EXIT, 2);


    public EditUserController(String passwordFormat, User userToEdit) {
        this(passwordFormat, userToEdit, null);
    }

    public EditUserController(String passwordFormat, User userToEdit, IUserDecorator decorator) {
        //take this out into an activity
        view = new UserForm("Edit User", decorator);
        view.setPasswordMode(passwordFormat);
        view.addCommand(CMD_SAVE);
        view.addCommand(CMD_CANCEL);
        view.setCommandListener(this);

        view.loadUser(userToEdit);
    }

    public void setTransitions (EditUserTransitions transitions) {
        this.transitions = transitions;
    }

    public void start() {
        J2MEDisplay.setView(view);
    }

    public void commandAction(Command c, Displayable d) {
        CrashHandler.commandAction(this, c, d);
    }

    public void _commandAction(Command c, Displayable d) {
        if(c.equals(CMD_SAVE)) {
            UserValidator validator = new UserValidator(view);
            int status = validator.validateUserEdit();
            if(status == UserValidator.OK) {
                User u = validator.constructUser();
                //Save to RMS?
                transitions.userEdited(u);
            } else {
                validator.handleInvalidUser(status, this);
            }
        } else if(c.equals(CMD_CANCEL)) {
            transitions.cancel();
        }
    }

}
