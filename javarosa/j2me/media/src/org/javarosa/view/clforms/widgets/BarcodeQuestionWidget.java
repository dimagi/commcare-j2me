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

package org.javarosa.view.singlequestionscreen.widgets;

import org.javarosa.barcode.acquire.BarcodeCaptureScreen;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.formmanager.view.singlequestionscreen.acquire.AcquireScreen;
import org.javarosa.formmanager.view.singlequestionscreen.acquire.AcquiringQuestionScreen;
import org.javarosa.j2me.services.BarcodeCaptureService;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.TextField;

import de.enough.polish.ui.Style;

/**
 * @author mel
 *
 *         A SingleQuestionScreen-syle widget that adds a scan command for
 *         scaning a barcode
 *
 */
public class BarcodeQuestionWidget extends AcquiringQuestionScreen {

    protected TextField tf;
    protected BarcodeCaptureService barcodeProcessor;

    public BarcodeQuestionWidget(FormEntryPrompt prompt, String groupName, Style style) {
        super(prompt, groupName,style);
    }

    public void createView() {

        //#if javarosa.usepolishlocalisation
        //setHint(Locale.get("hint.TypeOrScan"));
        //#else
        setHint("Type in your answer");
        //#endif

        //#style textBox
         tf = new TextField("", "", 200, TextField.ANY);
         if(qDef.instanceNode.required)
                tf.setLabel("*"+((QuestionDef)qDef.element).getLongText()); //visual symbol for required
                else
                    tf.setLabel(((QuestionDef)qDef.element).getLongText());
        this.append(tf);
        this.addNavigationButtons();
        if (((QuestionDef)qDef.element).getHelpText()!=null){
            setHint(((QuestionDef)qDef.element).getHelpText());
        }

    }

    public Command getAcquireCommand() {
        Command acquire;
        //#if javarosa.usepolishlocalisation
        // acquire =  new Command(Locale.get( "menu.Scan"), Command.SCREEN, 3);
        //#else
         acquire = new Command("Scan", Command.SCREEN, 3);
        //#endif
        return acquire;
    }

    public AcquireScreen getAcquireScreen(CommandListener callingListener) {
        String title = "Scan barcode";
        //#if javarosa.usepolishlocalisation
        //title = Locale.get( "title.ScanBarcode");
        //#endif
        BarcodeCaptureScreen bcScreen = new BarcodeCaptureScreen(
                title, this, callingListener);
        bcScreen.setBarcodeProcessor(barcodeProcessor);
        return bcScreen;
    }

    public IAnswerData getWidgetValue() {
        String s = tf.getString();
        return (s == null || s.equals("") ? null : new StringData(s));
    }

    protected void updateDisplay() {
        tf.setString(((StringData) acquiredData).getDisplayText());
    }

    public void setBarcodeProcessor(BarcodeCaptureService barcodeProcessor) {
        this.barcodeProcessor = barcodeProcessor;

    }

}