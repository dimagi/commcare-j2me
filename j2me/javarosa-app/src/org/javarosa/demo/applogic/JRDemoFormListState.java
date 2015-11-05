package org.javarosa.demo.applogic;

import org.javarosa.core.api.State;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.demo.activity.formlist.JRDemoFormListController;
import org.javarosa.demo.activity.formlist.JRDemoFormListTransitions;
import org.javarosa.demo.properties.DemoAppProperties;
import org.javarosa.demo.util.JRDemoUtil;
import org.javarosa.services.properties.api.PropertyUpdateState;
import org.javarosa.user.api.CreateUserState;
import org.javarosa.core.model.User;

public class JRDemoFormListState implements JRDemoFormListTransitions, State {

    public void start() {
        JRDemoFormListController ctrl = new JRDemoFormListController();
        ctrl.setTransitions(this);
        ctrl.start();
    }

    public void formSelected(int formID) {
        FormInstance formInstance = JRDemoUtil.getSavedFormInstance(formID);
        new JRDemoFormEntryState(formID).start();
    }

    public void viewSaved() {
        new JRDemoSavedFormListState().start();
    }

    public void exit() {
        JRDemoUtil.exit();
    }

    public void settings() {
        new PropertyUpdateState () {
            public void done () {
                new JRDemoFormListState().start();
            }
        }.start();
    }

    public void addUser() {
        new CreateUserState () {
            public void cancel() {
                new JRDemoFormListState().start();
            }

            public void userCreated(User newUser) {
                new JRDemoFormListState().start();
            }
        }.start();
    }

    public void downloadForms() {
        new JRDemoGetFormListHTTPState(PropertyManager._().getSingularProperty(DemoAppProperties.FORM_URL_PROPERTY)).start();
    }
}
