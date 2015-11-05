/**
 *
 */
package org.javarosa.user.api;


import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.util.NoLocalizedTextException;
import org.javarosa.j2me.log.CrashHandler;
import org.javarosa.j2me.log.HandledCommandListener;
import org.javarosa.j2me.util.media.ImageUtils;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.user.api.transitions.LoginTransitions;
import org.javarosa.core.model.User;
import org.javarosa.user.view.LoginForm;
import org.javarosa.utilities.media.MediaUtils;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;

import de.enough.polish.ui.Alert;
import de.enough.polish.ui.ImageItem;
import de.enough.polish.ui.StringItem;

/**
 * @author ctsims
 *
 */
public class LoginController implements HandledCommandListener {

    protected LoginTransitions transitions;

    protected LoginForm view;

    protected Alert demoModeAlert = null;
    protected String[] extraText;

    public LoginController() {
        this(null, true);
    }
    public LoginController(String[] extraText, String passwordFormat, boolean showDemo) {
        this.extraText = extraText;
        view = new LoginForm(Localization.get("form.login.login"), this.extraText, showDemo, true);
        view.setCommandListener(this);
        view.setPasswordMode(passwordFormat);
    }

    public LoginController(String title, String image, String[] extraText, String passwordFormat, boolean showDemo, boolean imagesEnabled) {
        this.extraText = extraText;
        view = new LoginForm(null, this.extraText, showDemo, true, imagesEnabled);
        view.setCommandListener(this);
        view.setPasswordMode(passwordFormat);

        if(image != null) {
            //#style loginImage?
            view.append(Graphics.TOP, new ImageItem(null, ImageUtils.getImage(image), ImageItem.LAYOUT_CENTER, ""));
        }

        if(title != null) {
            //#style loginTitle?
            view.append(Graphics.TOP, new StringItem(null, title));
        }
    }

    public LoginController(String[] extraText, boolean showDemo) {
        this(extraText,CreateUserController.PASSWORD_FORMAT_NUMERIC,showDemo);
    }

    public void setTransitions (LoginTransitions transitions) {
        this.transitions = transitions;
    }

    public void start() {
        J2MEDisplay.setView(view);
    }

    public void commandAction(Command c, Displayable d) {
        CrashHandler.commandAction(this, c, d);
    }

    // listener for the display
    public void _commandAction(Command c, Displayable d) {
        if (c == LoginForm.CMD_CANCEL_LOGIN) {
            transitions.exit();
        } else if (c == LoginForm.CMD_LOGIN_BUTTON) {
            if (this.view.validateUser()) {
                transitions.loggedIn(view.getLoggedInUser(), view.getPassWord());
                return;
            }
            performCustomUserValidation();

        } else if(c == LoginForm.CMD_TOOLS) {
            transitions.tools();
        }

        //#if javarosa.login.demobutton
        else if (c == LoginForm.CMD_DEMO_BUTTON) {
            try{
                MediaUtils.playAudio(Localization.get("demo.warning.filepath"));
            }
            catch(NoLocalizedTextException e){}
            demoModeAlert = J2MEDisplay.showError(null, Localization.get("activity.login.demomode.intro"), null, null, this);
        } else if (d == demoModeAlert) {
            // returning from demo mode warning popup
            User u = User.FactoryDemoUser();
            transitions.loggedIn(u, null);
        }
        //#endif
    }


    // this method is to be overridden by users.
    // handle errors here.
    protected void performCustomUserValidation() {
        J2MEDisplay.showError(Localization.get("activity.login.loginincorrect"), Localization.get("activity.login.tryagain"));
    }

}
