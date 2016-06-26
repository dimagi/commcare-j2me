package org.javarosa.formmanager.view.singlequestionscreen.screen;

import org.javarosa.core.services.locale.Localization;
import org.javarosa.formmanager.view.singlequestionscreen.Constants;

import javax.microedition.lcdui.Command;

import de.enough.polish.ui.FramedForm;
import de.enough.polish.ui.Item;
import de.enough.polish.ui.StringItem;
import de.enough.polish.ui.StyleSheet;

public class NewRepeatScreen extends FramedForm {

    public static Command noCommand = new Command("Yes",
            Command.ITEM, 1);
    public static Command yesCommand = new Command("No",
            Command.ITEM, 1);
    private String promptText="";

    public NewRepeatScreen(String promptText) {
        super(Localization.get("formview.repeat.addNew"), StyleSheet.getStyle(Constants.STYLE_TRANSITION_FLASH));
        this.promptText=promptText;
        createView();
        addCommands();
    }

    /**
     * Add initial view items to form
     */
    protected void createView() {

        //#style questiontext
        StringItem addNewQuestion = new StringItem(null,promptText,
                Item.PLAIN);
        //#style button
        StringItem yesItem = new StringItem(null, Localization
                .get("yes"), Item.BUTTON);
        yesItem.setDefaultCommand(yesCommand);
        //#style button
        StringItem noItem = new StringItem(null, Localization.get("no"),
                Item.BUTTON);
        noItem.setDefaultCommand(noCommand);

        this.append(addNewQuestion);
        this.append(yesItem);
        this.append(noItem);
    }

    /**
     * Add generic commands
     */
    private void addCommands() {
        this.addCommand(yesCommand);
        this.addCommand(noCommand);
    }
}
