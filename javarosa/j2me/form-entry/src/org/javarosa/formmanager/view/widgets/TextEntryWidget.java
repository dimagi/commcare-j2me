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

package org.javarosa.formmanager.view.widgets;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.condition.pivot.StringLengthRangeHint;
import org.javarosa.core.model.condition.pivot.UnpivotableExpressionException;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.data.helper.InvalidDataException;
import org.javarosa.core.services.Logger;
import org.javarosa.form.api.FormEntryPrompt;

import de.enough.polish.ui.Item;
import de.enough.polish.ui.TextField;

public class TextEntryWidget extends ExpandedWidget {
    int inputMode;

    IWidgetComponentWrapper wec;

    private TextField textField;

    public TextEntryWidget () {
        //#if polish.TextField.useDirectInput == true && !polish.blackberry
        this(TextField.MODE_UPPERCASE);
        //#else
        //# this(TextField.ANY);
        //#endif
    }

    public TextEntryWidget (int inputMode) {
        this.inputMode = inputMode;

        //#style textBox
        textField = new TextField("", "", 200, TextField.ANY);

        textField.setInputMode(inputMode);
        //#if device.identifier == Sony-Ericsson/P1i
        wec = new WidgetEscapeComponent();
        //#else
        wec = new EmptyWrapperComponent();
        //#endif
    }

    public void setConstraint(int constraint) {
        textField.setConstraints(textField.getConstraints() | constraint);
    }

    public int getNextMode () {
        return wec.wrapNextMode(ExpandedWidget.NEXT_ON_SELECT);
    }

    protected Item getEntryWidget (FormEntryPrompt prompt) {

        //See if we can get the max length of the constraint to limit entry options
        try {
            textField.setMaxSize(guessMaxStringLength(prompt));
        } catch (UnpivotableExpressionException e) {
            // No big deal
        } catch(Exception e) {
            // Not a vital feature for now, don't break on its account
            Logger.exception("pivot", e);
        }

        return wec.wrapEntryWidget(textField);
    }

    protected int guessMaxStringLength(FormEntryPrompt prompt) throws UnpivotableExpressionException{
        StringLengthRangeHint hint = new StringLengthRangeHint();
        prompt.requestConstraintHint(hint);

        StringData maxexample = hint.getMax();

        if(maxexample != null) {
            int length = maxexample.uncast().getString().length();
            if(!hint.isMaxInclusive()) {
                length -= 1;
            }
            return length;
        }
        throw new UnpivotableExpressionException();
    }

    public Item getInteractiveWidget() {
        return wec.wrapInteractiveWidget(super.getInteractiveWidget());
    }

    protected TextField textField () {
        return textField;
    }

    protected void updateWidget (FormEntryPrompt prompt) { /* do nothing */ }

    protected void setWidgetValue (Object o) {
        textField().setString((String)o);
    }

    protected IAnswerData getWidgetValue () throws InvalidDataException {
        String s = textField().getString().trim();
        return (s == null || s.equals("") ? null : new StringData(s));
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.formmanager.view.chatterbox.widget.IWidgetStyle#widgetType()
     */
    public int widgetType() {
        return Constants.CONTROL_INPUT;
    }

    protected IAnswerData getAnswerTemplate() {
        return new StringData();
    }
}