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

package org.javarosa.formmanager.view;

import org.javarosa.core.model.FormIndex;
import org.javarosa.formmanager.api.FormMultimediaController;


/**
 *
 * A view for displaying a form to the user, and allowing them to fill
 * out values.
 *
 * @author Drew Roos
 *
 */

//this is generic enough to be renamed 'IActivityView'
public interface IFormEntryView {

    /* form entry views MUST call controller.setFormEntryView(this) in their constructors before they
     * make any calls to the controller!
     */

    public void destroy ();

    public void show();

    /**
     * Show the form and set the focus to the specified index.
     * @param index
     */
    public void show(FormIndex index);

    public void attachFormMediaController(FormMultimediaController mediacontroller);
}
