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
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.form.api.FormEntryPrompt;

import de.enough.polish.ui.Item;
import de.enough.polish.ui.StringItem;

/**
 * TODO: make an expandedwidget.
 *
 * @author ctsims
 *
 */
public class MessageWidget extends ExpandedWidget {
    private StringItem ok;

    public MessageWidget (boolean showButton) {
        if(showButton) {
            //#style button
            ok = new StringItem(null, Localization.get("button.Next"));
        } else {
            //#style invisibleTrigger
            ok = new StringItem(null, "");
        }
    }

    public IAnswerData getData () {
        return new StringData("OK");
    }

    public int getNextMode () {
        return ExpandedWidget.NEXT_ON_MANUAL;
    }


    public int widgetType() {
        return Constants.CONTROL_TRIGGER;
    }

    protected IAnswerData getAnswerTemplate() {
        return new StringData();
    }

    protected Item getEntryWidget(FormEntryPrompt prompt) {
        return ok;
    }

    protected void updateWidget(FormEntryPrompt prompt) {
        // TODO Auto-generated method stub

    }

    protected void setWidgetValue(Object o) {
        //Nope. Not a thing.
    }

    protected IAnswerData getWidgetValue() {
        return new StringData("OK");
    }
}
