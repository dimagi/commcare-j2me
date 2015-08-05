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
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.TimeData;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.Date;

import de.enough.polish.ui.DateField;
import de.enough.polish.ui.Item;

public class TimeEntryWidget extends ExpandedWidget {

    public TimeEntryWidget() {
        super();
    }

    public int getNextMode () {
        return ExpandedWidget.NEXT_ON_ENTRY;
    }

    protected Item getEntryWidget (FormEntryPrompt prompt) {
        //#style textBox
        return new DateField(null, DateField.TIME);
    }

    private DateField dateField () {
        return (DateField)entryWidget;
    }

    protected void updateWidget (FormEntryPrompt prompt) { /* do nothing */ }

    protected void setWidgetValue (Object o) {
        dateField().setDate((Date)o);
    }

    protected IAnswerData getWidgetValue () {
        Date d = dateField().getDate();
        if(d == null) {
            return null;
        }
        return new TimeData(d);
    }

    /*
     * (non-Javadoc)
     * @see org.javarosa.formmanager.view.chatterbox.widget.IWidgetStyle#widgetType()
     */
    public int widgetType() {
        return Constants.CONTROL_INPUT;
    }

    protected IAnswerData getAnswerTemplate() {
        return new TimeData();
    }
}