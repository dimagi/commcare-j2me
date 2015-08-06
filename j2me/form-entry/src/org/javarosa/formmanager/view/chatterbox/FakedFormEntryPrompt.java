package org.javarosa.formmanager.view.chatterbox;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.formmanager.view.IQuestionWidget;

import java.util.Vector;

public class FakedFormEntryPrompt extends FormEntryPrompt {

    private String text;
    private int controlType;
    private int dataType;

    private Vector<SelectChoice> choices;

    public FakedFormEntryPrompt(String text, int controlType, int dataType) {
        this.text = text;
        this.controlType = controlType;
        this.dataType = dataType;
        choices = new Vector<SelectChoice>();
    }

    public String getAnswerText() {
        return null;
    }

    public IAnswerData getAnswerValue() {
        return null;
    }

    public String getConstraintText() {
        return null;
    }

    public int getControlType() {
        return controlType;
    }

    public int getDataType() {
        return dataType;
    }

    public String getHelpText() {
        return null;
    }

    public String getLongText(String textID) {
        return text;
    }

    public String getPromptAttributes() {
        return null;
    }

    public Vector<SelectChoice> getSelectChoices() {
        return choices;
    }

    public void addSelectChoice(SelectChoice choice) {
        choice.setIndex(choices.size());
        choices.addElement(choice);
    }

    public String getShortText(String textID) {
        return text;
    }

    public boolean isReadOnly() {
        return false;
    }

    public boolean isRequired() {
        return true;
    }

    //==== observer pattern ====//

    public void register (IQuestionWidget widget) {
        //do nothing -- this fake prompt is not bound to anything real in the instance
    }

    public void unregister () {
        //do nothing -- this fake prompt is not bound to anything real in the instance
    }



    //Ugly hack.
    public String getQuestionText(String textID){
        return text;
    }

    //Ugly hack.
    public String getSpecialFormSelectItemText(Selection sel,String form){
        if(sel == null)return null;
        if(form != null || form != "long" || form != "short" || form != "") return null;
        return sel.choice.getLabelInnerText();
    }

    public String getSpecialFormSelectChoiceText(SelectChoice sel,String form){
        return getSpecialFormSelectItemText(sel.selection(),form);
    }

    //Hey look, another hack.
    public String getSelectItemText(Selection sel){
        return sel.choice.getLabelInnerText();
    }
}
