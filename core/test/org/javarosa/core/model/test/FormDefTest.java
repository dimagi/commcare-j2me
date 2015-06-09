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

package org.javarosa.core.model.test;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

import org.javarosa.core.model.FormIndex;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.form.api.FormEntryController;

/**
 * @author Phillip Mates
 */
public class FormDefTest extends TestCase {

    // How many tests does the suite have?
    // Used to dispatch in doTest's switch statement.
    public final static int NUM_TESTS = 3;

    public FormDefTest(String name, TestMethod rTestMethod) {
        super(name, rTestMethod);
    }

    public FormDefTest(String name) {
        super(name);
    }

    public FormDefTest() {
        super();
    }

    public Test suite() {
        TestSuite aSuite = new TestSuite();

        for (int i = 1; i <= NUM_TESTS; i++) {
            final int testID = i;
            aSuite.addTest(new FormDefTest("FormDef Test " + i, new TestMethod() {
                public void run(TestCase tc) {
                    ((FormDefTest)tc).doTest(testID);
                }
            }));
        }

        return aSuite;
    }

    public void doTest(int i) {
        switch (i) {
            case 1:
                testAnswerConstraint();
                break;
            case 2:
                testCurrentFuncInTriggers();
                break;
            case 3:
                testSetValuePredicate();
                break;
        }
    }


    /**
     * Make sure that 'current()' expands correctly when used in conditionals
     * such as in 'relevant' tags. The test answers a question and expects the
     * correct elements to be re-evaluated and set to not relevant.
     */
    public void testCurrentFuncInTriggers() {
        FormParseInit fpi = new FormParseInit("/trigger_and_current_tests.xml");

        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        do {
            QuestionDef q = fpi.getCurrentQuestion();
            if (q == null) {
                continue;
            }
            // get the reference of question
            TreeReference qRef = (TreeReference)((XPathReference)q.getBind()).getReference();

            // are we changing the value of /data/show?
            if (qRef.toString().equals("/data/show")) {
                int response = fec.answerQuestion(new StringData("no"));
                if (response != fec.ANSWER_OK) {
                    fail("Bad response from fec.answerQuestion()");
                }
            } else if (q.getID() == 2) {
                // check (sketchily) if the second question is shown, which
                // shouldn't happen after answering "no" to the first, unless
                // triggers aren't working properly.
                fail("shouldn't be relevant after answering no before");
            }
        } while (fec.stepToNextEvent() != fec.EVENT_END_OF_FORM);
    }

    public void testAnswerConstraint() {
        IntegerData ans = new IntegerData(13);
        FormParseInit fpi = new FormParseInit("/ImageSelectTester.xhtml");
        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        do {
            QuestionDef q = fpi.getCurrentQuestion();
            if (q == null || q.getTextID() == null || q.getTextID() == "") {
                continue;
            }
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
    
    /**
     * Test setvalue expressions which have predicate references
     */
    public void testSetValuePredicate() {
        FormParseInit fpi = new FormParseInit("/test_setvalue_predicate.xml");
        FormEntryController fec = fpi.getFormEntryController();
        fpi.getFormDef().initialize(true,null);
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        boolean testPassed = false;
        do {
            if(fec.getModel().getEvent() != FormEntryController.EVENT_QUESTION) {
                continue;
            }
            String text = fec.getModel().getQuestionPrompt().getQuestionText();
            //check for our test
            if(text.indexOf("Test") != -1) {
                if(text.indexOf("pass") != -1) {
                    testPassed = true;
                }
            }
            
        } while (fec.stepToNextEvent() != fec.EVENT_END_OF_FORM);
        if(!testPassed) {
            fail("Setvalue Predicate Target Test");
        }
    }

}
