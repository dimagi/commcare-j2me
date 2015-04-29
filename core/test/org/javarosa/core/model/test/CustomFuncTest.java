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

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.xpath.XPathUnhandledException;

import java.util.Vector;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

/**
 * @author Will Pride
 */
public class CustomFuncTest extends TestCase {
    FormParseInit fpi = null;

    public final static int NUM_TESTS = 2;

    public CustomFuncTest(String name, TestMethod rTestMethod) {
        super(name, rTestMethod);
        initForm();
    }

    public CustomFuncTest(String name) {
        super(name);
        initForm();
    }

    public CustomFuncTest() {
        super();
        initForm();
    }

    public void initForm() {
        fpi = new FormParseInit();
        fpi.setFormToParse("/CustomFunctionTest.xhtml");
    }

    public Test suite() {
        TestSuite aSuite = new TestSuite();

        for (int i = 1; i <= NUM_TESTS; i++) {
            final int testID = i;
            aSuite.addTest(new CustomFuncTest("Custom Function Test " + i, new TestMethod() {
                public void run(TestCase tc) {
                    ((CustomFuncTest)tc).doTest(testID);
                }
            }));
        }

        return aSuite;
    }

    public void doTest(int i) {
        switch (i) {
            case 1:
                testFormFailure();
                break;
            case 2:
                testFormSuccess();
                break;
        }
    }

    /**
     * Try to use a form that has a custom function defined without extending
     * the context with a custom function handler.
     */
    public void testFormFailure() {

        String formName = new String("/CustomFunctionTest.xhtml");
        fpi.setFormToParse(formName);

        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        do {
            QuestionDef q = fpi.getCurrentQuestion();
            if (q == null) {
                continue;
            }

            try {
                fec.answerQuestion(new IntegerData(1));
            } catch (XPathUnhandledException e) {
                System.out.println("Caught exception: " + e + " + which is good.");
                return;
            }
            fail("Should have failed parsing here");

        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
    }

    /**
     * Successfully use a form that has a custom function by extending the
     * context with a custom function handler.
     */
    public void testFormSuccess() {
        String formName = new String("/CustomFunctionTest.xhtml");
        fpi.setFormToParse(formName);

        fpi.getFormDef().exprEvalContext.addFunctionHandler(new IFunctionHandler() {
            public String getName() {
                return "my_double";
            }

            public Object eval(Object[] args, EvaluationContext ec) {
                Double my_double = (Double)args[0];
                assertEquals(new Double(2.0), new Double(my_double.doubleValue() * 2));
                return new Double(my_double.doubleValue() * 2);
            }

            public Vector getPrototypes() {
                Class[] proto = {Double.class};
                Vector<Class[]> v = new Vector<Class[]>();
                v.addElement(proto);
                return v;
            }

            public boolean rawArgs() {
                return false;
            }

            public boolean realTime() {
                return false;
            }
        });

        FormEntryController fec = fpi.getFormEntryController();

        do {

            QuestionDef q = fpi.getCurrentQuestion();
            if (q == null) {
                continue;
            }
            fec.answerQuestion(new IntegerData(1));

        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
    }
}
