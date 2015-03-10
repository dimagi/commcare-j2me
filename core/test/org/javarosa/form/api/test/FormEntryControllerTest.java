/*
 * Copyright (C) 2015 JavaRosa
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

package org.javarosa.form.api;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

import org.javarosa.core.model.FormElementStateListener;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.IDataReference;
import org.javarosa.core.model.IFormElement;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;

/**
 *
 * @author Phillip Mates
 */
public class FormEntryControllerTest extends TestCase {
    private FormParseInit fpi;
    private FormEntryModel femodel;
    private FormEntryController fec;

    private String formName = new String("/test_form_entry_controller.xml");

    public FormEntryControllerTest() {
        super();
        fpi = new FormParseInit(formName);
        fpi.setFormToParse();
    }

    public Test suite() {
        TestSuite aSuite = new TestSuite();

        aSuite.addTest(new FormEntryControllerTest("FormEntryController Test .answerQuestion", new TestMethod() {
            public void run(TestCase tc) {
                ((FormEntryControllerTest) tc).testAnswerQuestion();
            }
        }));

        return aSuite;
    }

    public void testAnswerQuestion() {
        IntegerData ans = new IntegerData(13);

        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        do {
            // get current question
            QuestionDef q = fpi.getCurrentQuestion();

            if (q == null || q.getTextID() == null || q.getTextID() == "") {
                continue;
            }

            // complex question w/o constraint
            // complex question w/ constraint
            // simple question w/o constraint
            // simple question w/ constraint pass
            // simple question w/ constraint fail
            if (q.getTextID().equals("constraint-test")) {
                int response = fec.answerQuestion(ans);
                if (response == fec.ANSWER_CONSTRAINT_VIOLATED) {
                    fail("Answer Constraint test failed.");
                } else if (response == fec.ANSWER_OK) {
                    break;
                } else {
                    fail("Bad response from fec.answerQuestion()");
                }
            }
        } while (fec.stepToNextEvent() != fec.EVENT_END_OF_FORM);
    }
}
