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

import org.javarosa.core.model.condition.pivot.IntegerRangeHint;
import org.javarosa.core.model.condition.pivot.UnpivotableExpressionException;
import org.javarosa.core.model.data.DecimalData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.LongData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.data.helper.InvalidDataException;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.form.api.FormEntryPrompt;

import de.enough.polish.ui.Item;
import de.enough.polish.ui.TextField;

public class NumericEntryWidget extends TextEntryWidget {
    private boolean isDecimal;
    IAnswerData template;

    public NumericEntryWidget() {
        this(false, new IntegerData());
    }

    public NumericEntryWidget(boolean dec, IAnswerData template) {
        super();
        this.isDecimal = dec;
        this.template = template;
    }

    protected Item getEntryWidget (FormEntryPrompt prompt) {
        TextField tf = textField();
        int clearNumberType = tf.getConstraints() & ~(TextField.DECIMAL + TextField.NUMERIC);
        tf.setConstraints( clearNumberType | (isDecimal ? TextField.DECIMAL : TextField.NUMERIC));

        return super.getEntryWidget(prompt);
    }

    protected void setWidgetValue (Object o) {
        template.setValue(o);
        super.setWidgetValue(template.uncast().getString());
    }

    protected IAnswerData getWidgetValue () throws InvalidDataException {
        String s = textField().getString().trim();
        if (s == null || s.equals("")) {
            return null;
        }
        try {
            return template.cast(new UncastData(s));
        } catch (IllegalArgumentException iae) {
            String message;
            //See if we can provide good details
            if(template instanceof LongData) {
                message = Localization.get("form.entry.badnum.long", new String[] {s});
            } else if(template instanceof IntegerData) {
                message = Localization.get("form.entry.badnum.int", new String[] {s});
            } else if(template instanceof DecimalData) {
                message = Localization.get("form.entry.badnum.dec", new String[] {s});
            } else {
                message = Localization.get("form.entry.badnum", new String[] {s, template.getClass().getName()});
            }
            throw new InvalidDataException(message, new UncastData(s));
        }
    }

    protected IAnswerData getAnswerTemplate() {
        return template;
    }

    protected int guessMaxStringLength(FormEntryPrompt prompt) throws UnpivotableExpressionException{
        //Awful. Need factory for this
        //TODO: Negative numbers?
        if(template instanceof IntegerData) {
            IntegerRangeHint hint = new IntegerRangeHint();
            prompt.requestConstraintHint(hint);

            IntegerData maxexample = hint.getMax();
            IntegerData minexample = hint.getMin();

            if(minexample != null) {
                if(((Integer)minexample.getValue()).intValue() < 0) {
                    throw new UnpivotableExpressionException();
                }
            }

            if(maxexample != null) {
                int max = ((Integer)maxexample.getValue()).intValue();
                if(!hint.isMaxInclusive()) {
                    max -= 1;
                }
                return String.valueOf(max).length();
            }
        }
        throw new UnpivotableExpressionException();
    }
}