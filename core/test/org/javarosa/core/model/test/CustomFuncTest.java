package org.javarosa.core.model.test;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.xpath.XPathArityException;
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
    private FormParseInit fpi;

    public final static int NUM_TESTS = 3;

    public CustomFuncTest(String name, TestMethod rTestMethod) {
        super(name, rTestMethod);
    }

    public CustomFuncTest(String name) {
        super(name);
    }

    public CustomFuncTest() {
        super();
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
            case 3:
                testFormOverride();
                break;
        }
    }

    /**
     * Try to use a form that has a custom function defined without extending
     * the context with a custom function handler.
     */
    public void testFormFailure() {
        fpi = new FormParseInit("/CustomFunctionTest.xhtml");

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
                // we expect the test to fail on parsing
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
        fpi = new FormParseInit("/CustomFunctionTest.xhtml");

        // Custom func to double the numeric argument passed in.
        IFunctionHandler myDouble = new IFunctionHandler() {
            public String getName() {
                return "my_double";
            }

            public Object eval(Object[] args, EvaluationContext ec) {
                Double my_double = (Double)args[0];
                assertEquals(2.0, my_double * 2.0);
                return my_double * 2.0;
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
        };

        fpi.getFormDef().exprEvalContext.addFunctionHandler(myDouble);

        FormEntryController fec = fpi.getFormEntryController();

        do {
            QuestionDef q = fpi.getCurrentQuestion();
            if (q == null) {
                continue;
            }
            fec.answerQuestion(new IntegerData(1));
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
    }

    /**
     * Test overriding of built-in functions.
     * Behaviour should be:
     * - Use overridden function but if there's an arity mismatch fall through
     * to the default.
     * - Makes sure falling through to default still raises additional arity
     * mismatches.
     */
    public void testFormOverride() {
        fpi = new FormParseInit("/CustomFunctionTestOverride.xhtml");

        // Override true to take in one argument and return 4.0
        IFunctionHandler myTrue = new IFunctionHandler() {
            public String getName() {
                return "true";
            }

            public Object eval(Object[] args, EvaluationContext ec) {
                if (args.length != 1) {
                    throw new XPathArityException(getName(), 1, args.length);
                }
                return 4.0;
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
        };

        fpi.getFormDef().exprEvalContext.addFunctionHandler(myTrue);

        FormEntryController fec = fpi.getFormEntryController();

        boolean sawQuestionThree = false;
        do {
            QuestionDef q = fpi.getCurrentQuestion();
            if (q == null) {
                continue;
            }
            if ("qOne".equals(q.getTextID())) {
                fec.answerQuestion(new IntegerData(1));
            } else if ("qTwo".equals(q.getTextID())) {
                try {
                    fec.answerQuestion(new IntegerData(2));
                } catch (XPathArityException e) {
                    // we expect the test to fail on parsing, since it triggers
                    // a calculation that sends too many args to the overriden
                    // 'true' function
                }
            } else if (q.getID() == 3) {
                // we expect calling "true()" will default to old behavior
                sawQuestionThree = true;
            } else if (q.getID() == 4 && sawQuestionThree) {
                // we should've seen the last 2 question in the form
                return;
            }
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
        fail("error in form expression calculation; the last form" +
                " question should be relevant");
    }
}
