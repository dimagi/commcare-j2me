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

package org.javarosa.demo.util;

import org.javarosa.core.services.PropertyManager;
import org.javarosa.formmanager.api.JrFormEntryController;
import org.javarosa.formmanager.properties.FormManagerProperties;
import org.javarosa.formmanager.view.IFormEntryView;
import org.javarosa.formmanager.view.IFormEntryViewFactory;
import org.javarosa.formmanager.view.chatterbox.Chatterbox;
import org.javarosa.formmanager.view.singlequestionscreen.SingleQuestionView;


public class JRDemoFormEntryViewFactory implements IFormEntryViewFactory {

    String title;

    public JRDemoFormEntryViewFactory(String title) {
        this.title = title;
    }
    public IFormEntryView getFormEntryView (JrFormEntryController controller) {
        String viewType = PropertyManager._().getSingularProperty(FormManagerProperties.VIEW_TYPE_PROPERTY);

        if (FormManagerProperties.VIEW_CHATTERBOX.equals(viewType)) {
            return new Chatterbox(title, controller);

        } else if (FormManagerProperties.VIEW_SINGLEQUESTIONSCREEN.equals(viewType)) {
            return new SingleQuestionView(controller);

        } else {
            throw new RuntimeException("No view known for type [" + viewType + "]");
        }
    }
}
