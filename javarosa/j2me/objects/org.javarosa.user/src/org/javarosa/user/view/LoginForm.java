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

package org.javarosa.user.view;

import org.javarosa.core.util.DataUtil;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.core.util.SHA1;
import org.javarosa.j2me.util.media.ImageUtils;
import org.javarosa.user.api.CreateUserController;
import org.javarosa.core.model.User;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Image;

import de.enough.polish.ui.FramedForm;
import de.enough.polish.ui.Item;
import de.enough.polish.ui.ItemStateListener;
import de.enough.polish.ui.StringItem;
import de.enough.polish.ui.TextField;

public class LoginForm extends FramedForm {

    private final static int DEFAULT_COMMAND_PRIORITY = 3;

    private ImageButton demoButton;
    private ImageButton loginButton;

    private StringItem regularDemoButton;
    private StringItem regularLoginButton;

    public final static Command CMD_DEMO_BUTTON = new Command(Localization.get("menu.Demo"),
            Command.ITEM, DEFAULT_COMMAND_PRIORITY);

    public final static Command CMD_CANCEL_LOGIN = new Command(Localization.get("menu.Exit"),
            Command.CANCEL, 2);
    public final static Command CMD_LOGIN_BUTTON = new Command(Localization.get("menu.Login"),
            Command.ITEM, DEFAULT_COMMAND_PRIORITY);

    public final static Command  CMD_TOOLS = new Command(Localization.get("menu.Tools"),
            Command.ITEM, 4);


    private TextField usernameField;
    private TextField passwordField;
    private IStorageUtility users;
    private User loggedInUser;

    private String[] extraText;
    private boolean demoEnabled = true;

    private boolean toolsEnabled;

    private boolean imagesEnabled = false;

    public LoginForm() {
            //#style loginView
        super(Localization.get("form.login.login"));
        init();
    }

    /**
     * @param title
     */
    public LoginForm(String title) {
        //#style loginView
        super(title);
        init();
    }

    /**
     *
     * @param title
     * @param extraText
     */
    public LoginForm(String title, String[] extraText, boolean demoEnabled, boolean toolsEnabled) {
        this(title, extraText, demoEnabled, toolsEnabled, false);
    }

    public LoginForm(String title, String[] extraText, boolean demoEnabled, boolean toolsEnabled, boolean imagesEnabled) {
        //#style loginView
        super(title);
        this.extraText = extraText;
        this.demoEnabled = demoEnabled;
        this.toolsEnabled = toolsEnabled;
        this.imagesEnabled = imagesEnabled;
        init();
    }

    /**
     * @param loginActivity
     */
    private void init() {
        this.users = StorageManager.getStorage(User.STORAGE_KEY);

        boolean recordsExist = this.users.getNumRecords() > 0;

        //#debug debug
        System.out.println("records in RMS:" + recordsExist);

        User tempUser = new User();
        tempUser.setUsername("");// ? needed

        if (recordsExist) {
            int highestID = -1;
            IStorageIterator ui = users.iterate();
            while (ui.hasMore()) {
                int userID = ui.nextID();
                if (highestID == -1 || userID > highestID)
                    highestID = userID;
            }

            tempUser = (User)users.read(highestID);

        }
        initLoginControls(tempUser.getUsername());

        showVersions();

    }

    /**
     * @param username
     */
    private void initLoginControls(String username) {

        // create the username and password input fields
        //#style loginTextFields
        this.usernameField = new TextField(Localization.get("form.login.username"), username, 50,
                TextField.ANY);
        //#style loginTextFields
        this.passwordField = new TextField(Localization.get("form.login.password"), "", 10,
                TextField.PASSWORD);

        // TODO:what this?
        addCommand(CMD_CANCEL_LOGIN);

        append(this.usernameField);
        append(this.passwordField);

        // set the focus on the password field
        this.focus(this.passwordField);
        this.passwordField.setDefaultCommand(CMD_LOGIN_BUTTON);
        this.passwordField.setItemStateListener(new ItemStateListener(){
            public void itemStateChanged(Item item) {
                if(passwordField.getString().length()>0){
                    if(imagesEnabled){
                        demoButton.setVisible(false);
                    }else{
                        regularDemoButton.setVisible(false);
                    }
                }
                else{
                    if(imagesEnabled){
                        demoButton.setVisible(true);
                    } else{
                        regularDemoButton.setVisible(true);
                    }
                }

            }
        });

        // add the login button
        if(imagesEnabled){
            Image mImage = ImageUtils.getImage(Localization.get("icon.login.path"));
            //#style myButton
            this.loginButton = new ImageButton(mImage);
            this.loginButton.setLayout(Item.LAYOUT_CENTER);
            this.loginButton.setLayout(Item.LAYOUT_CENTER);
            append(this.loginButton);
            this.loginButton.setDefaultCommand(CMD_LOGIN_BUTTON);
        }
        else{
            this.regularLoginButton = new StringItem(null, Localization.get("form.login.login"), Item.BUTTON);
             append(this.regularLoginButton);
             this.regularLoginButton.setDefaultCommand(CMD_LOGIN_BUTTON);
        }

        //#if javarosa.login.demobutton
        if (demoEnabled) {
            if(imagesEnabled){
                Image mImage = ImageUtils.getImage(Localization.get("icon.demo.path"));
                //#style myButton
                this.demoButton = new ImageButton(mImage);
                append(this.demoButton);
                this.demoButton.setLayout(Item.LAYOUT_CENTER);
                this.demoButton.setDefaultCommand(CMD_DEMO_BUTTON);
            }
            else{
                this.regularDemoButton = new StringItem(null, Localization.get("menu.Demo"), Item.BUTTON);
                 append(this.regularDemoButton);
                 this.regularDemoButton.setDefaultCommand(CMD_DEMO_BUTTON);
            }
        }
        //#endif

        if(toolsEnabled) {
            addCommand(CMD_TOOLS);
        }
        // put the extra text if it's been set
        if(this.extraText != null) {
            for (int i = 0; i < extraText.length; i++) {
                //#style loginExtraText?
                append(this.extraText[i]);
            }
        }
    }

    /**
     *
     */
    private void showVersions() {
        //#if javarosa.login.showbuild
        //String ccv = (String) this.parent.getActivityContext().getElement(
        //        CTX_COMMCARE_VERSION);
        //String ccb = (String) this.parent.getActivityContext().getElement(
        //        CTX_COMMCARE_BUILD);
        //String jrb = (String) this.parent.getActivityContext().getElement(
        //        CTX_JAVAROSA_BUILD);

        //if (ccv != null && !ccv.equals("")) {
        //    this.append(Localization.get("form.login.commcareversion") + " " + ccv);
        //}
        //if ((ccb != null && !ccb.equals(""))
        //        && (jrb != null && !jrb.equals(""))) {
        //    this.append(Localization.get("form.login.buildnumber") + " " + ccb + "-" + jrb);
        //}
        //#endif
    }

    /**
     *
     * After login button is clicked, activity asks form to validate user
     *
     * @return
     */
    public boolean validateUser() {
        boolean superUserLogin = false;
        //#ifdef superuser-login.enable:defined
        //#if superuser-login.enable
        //#    superUserLogin = true;
        //#endif
        //#endif

        String usernameEntered = this.usernameField.getString().trim();
        String passwordEntered = this.passwordField.getString().trim();

        IStorageIterator ui = users.iterate();
        while (ui.hasMore()) {
            User u = (User)ui.nextRecord();

            String xName = u.getUsername();
            String xPass = u.getPasswordHash();
            String xType = u.getUserType();

            if (   (xName.equalsIgnoreCase(usernameEntered) ||
                    (superUserLogin && xType.equals(User.ADMINUSER)))
                 && checkPassword(xPass, passwordEntered)) {
                setLoggedInUser(u);
                return true;
            }
        }

        return false;

    }

    private boolean checkPassword(String stored, String input) {
        if(stored.indexOf("$") != -1) {
            String alg = "sha1";
            String salt = (String)DataUtil.split(stored,"$", false).elementAt(1);
            String hashed = SHA1.encodeHex(salt + input);
            String compare = alg + "$" + salt + "$" + hashed;

            return stored.equalsIgnoreCase(compare);
        }
        else {
            return stored.equalsIgnoreCase(input);
        }

    }

    /**
     * @param passwordMode
     */
    public void setPasswordMode(String passwordMode) {
        if (CreateUserController.PASSWORD_FORMAT_NUMERIC.equals(passwordMode)) {
            if(passwordField.getConstraints() != (TextField.PASSWORD | TextField.NUMERIC)) {
            this.passwordField.setConstraints(TextField.PASSWORD
                    | TextField.NUMERIC);
            }
        } else if (CreateUserController.PASSWORD_FORMAT_ALPHA_NUMERIC
                .equals(passwordMode)) {
            if(this.passwordField.getConstraints() != TextField.PASSWORD) {
                this.passwordField.setConstraints(TextField.PASSWORD);
            }
        }
    }

    public String getPassWord() {
        return this.passwordField.getString();

    }

    public String getUserName() {
        return this.usernameField.getString();

    }

    public User getLoggedInUser() {
        return this.loggedInUser;
    }

    public void setLoggedInUser(User loggedInUser) {
        this.loggedInUser = loggedInUser;
    }

    public Object getScreenObject() {
        return this;
    }

    public TextField getPasswordField() {
        return this.passwordField;
    }

    // ------------------------

}
