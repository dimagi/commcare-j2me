package org.javarosa.demo.applogic;

import org.javarosa.demo.util.JRDemoUtil;
import org.javarosa.j2me.view.J2MEDisplay;
import org.javarosa.user.api.LoginState;
import org.javarosa.core.model.User;

public class JRDemoLoginState extends LoginState {

    public void start() {
        JRDemoContext._().setUser(null);
        super.start();
    }

    public void exit() {
        JRDemoUtil.exit();
    }

    public void loggedIn(User u, String password) {
        JRDemoContext._().setUser(u);
        new JRDemoFormListState().start();
    }

    public void tools() {
        //Not sure if we have anything here
        J2MEDisplay.showError("No Tools", "Tools not yet supported");
    }

}
