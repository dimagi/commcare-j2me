package org.javarosa.core.form.api.test;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.QuestionString;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.test.DummyFormEntryPrompt;
import org.javarosa.core.services.PrototypeManager;
import org.javarosa.core.services.locale.Localizer;
import org.javarosa.core.services.locale.TableLocaleSource;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;

public class TextFormTests extends TestCase {

    QuestionDef q = null;
    FormEntryPrompt fep = null;
    FormParseInit fpi = null;

    static PrototypeFactory pf;

    static {
        PrototypeManager.registerPrototype("org.javarosa.model.xform.XPathReference");
        pf = ExtUtil.defaultPrototypes();
    }


    public TextFormTests(String name, TestMethod rTestMethod) {
        super(name, rTestMethod);
        initStuff();
    }

    public TextFormTests(String name) {
        super(name);
        initStuff();
    }

    public TextFormTests() {
        super();
        initStuff();
    }

    public void initStuff() {
        fpi = new FormParseInit("/ImageSelectTester.xhtml");
        q = fpi.getFirstQuestionDef();
        fep = new FormEntryPrompt(fpi.getFormDef(), fpi.getFormEntryModel().getFormIndex());
    }


    public Test suite() {
        TestSuite aSuite = new TestSuite();
        System.out.println("Running TextFormTests...");
        for (int i = 1; i <= NUM_TESTS; i++) {
            final int testID = i;
            aSuite.addTest(new TextFormTests("TextForm Test Test " + i, new TestMethod() {
                public void run(TestCase tc) {
                    ((TextFormTests)tc).doTest(testID);
                }
            }));
        }

        return aSuite;
    }


    public final static int NUM_TESTS = 9;

    public void doTest(int i) {
        switch (i) {
            case 1:
                testConstructors();
                break;
            case 2:
                testPromptsNoLocalizer();
                break;
            case 3:
                testPromptIDsNoLocalizer();
                break;
            case 4:
                testPromptsWithLocalizer();
                break;
            case 5:
                testSelectChoicesNoLocalizer();
                break;
            case 6:
                testSelectChoiceIDsNoLocalizer();
                break;
            case 7:
                testNonLocalizedText();
                break;
            case 8:
                testTextForms();
                break;
        }
    }


    public void testConstructors() {
        QuestionDef q;

        q = new QuestionDef();
        if (q.getID() != -1) {
            fail("QuestionDef not initialized properly (default constructor)");
        }

        q = new QuestionDef(17, Constants.CONTROL_RANGE);
        if (q.getID() != 17) {
            fail("QuestionDef not initialized properly");
        }
        if (q.getControlType() != Constants.CONTROL_RANGE) {
            fail("QuestionDef not initialized properly");
        }
    }

    /**
     * Test that the long and short text forms work as expected
     * (fallback to default for example).
     * Test being able to retrieve other exotic forms
     */
    public void testTextForms() {
        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());
        Localizer l = fpi.getFormDef().getLocalizer();

        l.setDefaultLocale(l.getAvailableLocales()[0]);
        l.setLocale(l.getAvailableLocales()[0]);
        int state = fec.getModel().getEvent();
        while (state != FormEntryController.EVENT_QUESTION) {
            state = fec.stepToNextEvent();
        }
        fep = fec.getModel().getQuestionPrompt();

        if (!fep.getLongText().equals("Patient ID"))
            fail("getLongText() not returning correct value");
        if (!fep.getShortText().equals("ID")) fail("getShortText() not returning correct value");
        if (!fep.getAudioText().equals("jr://audio/hah.mp3"))
            fail("getAudioText() not returning correct value");

        state = -99;
        while (state != FormEntryController.EVENT_QUESTION) {
            state = fec.stepToNextEvent();
        }
        fep = fec.getModel().getQuestionPrompt();

        if (!fep.getLongText().equals("Full Name"))
            fail("getLongText() not falling back to default text form correctly, returned: " + fep.getLongText());
        if (fep.getSpecialFormQuestionText("long") != null)
            fail("getSpecialFormQuestionText() returning incorrect value");

    }

    public void testNonLocalizedText() {
        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());
        boolean testFlag = false;
        Localizer l = fpi.getFormDef().getLocalizer();

        l.setDefaultLocale(l.getAvailableLocales()[0]);
        l.setLocale(l.getAvailableLocales()[0]);

        do {
            if (fpi.getCurrentQuestion() == null) continue;
            QuestionDef q = fpi.getCurrentQuestion();
            fep = fpi.getFormEntryModel().getQuestionPrompt();
            String t = fep.getQuestionText();
            if (t == null) continue;
            if (t.equals("Non-Localized label inner text!")) testFlag = true;


        } while (fec.stepToNextEvent() != fec.EVENT_END_OF_FORM);

        if (!testFlag) fail("Failed to fallback to labelInnerText in testNonLocalizedText()");
    }

    public void testSelectChoiceIDsNoLocalizer() {

        QuestionDef q = fpi.getFirstQuestionDef();

        q.addSelectChoice(new SelectChoice("choice1 id", "val1"));
        q.addSelectChoice(new SelectChoice("loc: choice2", "val2", false));

        if (!fep.getSelectChoices().toString().equals("[{choice1 id} => val1, loc: choice2 => val2]")) {
            fail("Could not add individual select choice ID" + fep.getSelectChoices().toString());
        }


        //clean up
        q.removeSelectChoice(q.getChoices().elementAt(0));
        q.removeSelectChoice(q.getChoices().elementAt(0));
    }

    public void testSelectChoicesNoLocalizer() {
        QuestionDef q = fpi.getFirstQuestionDef();
        if (q.getNumChoices() != 0) {
            fail("Select choices not empty on init");
        }

//        fpi.getNextQuestion();

        String onetext = "choice";
        String twotext = "stacey's";
        SelectChoice one = new SelectChoice(null, onetext, "val", false);
        q.addSelectChoice(one);
        SelectChoice two = new SelectChoice(null, twotext, "mom", false);
        q.addSelectChoice(two);


        if (!fep.getSelectChoices().toString().equals("[choice => val, stacey's => mom]")) {
            fail("Could not add individual select choice" + fep.getSelectChoices().toString());
        }

        Object a = onetext;
        Object b = fep.getSelectChoiceText(one);
        this.assertEquals("Invalid select choice text returned", a, b);

        this.assertEquals("Invalid select choice text returned", twotext, fep.getSelectChoiceText(two));

        this.assertNull("Form Entry Caption incorrectly contains Image Text", fep.getSpecialFormSelectChoiceText(one, FormEntryCaption.TEXT_FORM_IMAGE));

        this.assertNull("Form Entry Caption incorrectly contains Audio Text", fep.getSpecialFormSelectChoiceText(one, FormEntryCaption.TEXT_FORM_AUDIO));

        q.removeSelectChoice(q.getChoice(0));
        q.removeSelectChoice(q.getChoice(0));
    }

    public void testPromptsWithLocalizer() {
        Localizer l = new Localizer();

        TableLocaleSource table = new TableLocaleSource();
        l.addAvailableLocale("locale");
        l.setDefaultLocale("locale");
        table.setLocaleMapping("prompt;long", "loc: long text");
        table.setLocaleMapping("prompt;short", "loc: short text");
        table.setLocaleMapping("help", "loc: help text");
        l.registerLocaleResource("locale", table);

        l.setLocale("locale");


        QuestionDef q = new QuestionDef();

        QuestionString helpString = new QuestionString("long");
        helpString.setTextId("help");

        q.putQuestionString("long", helpString);
        FormEntryPrompt fep = new DummyFormEntryPrompt(l, "prompt", q);

        if (!"loc: long text".equals(fep.getLongText())) {
            fail("Long text did not localize properly");
        }
        if (!"loc: short text".equals(fep.getShortText())) {
            fail("Short text did not localize properly");
        }

    }

    public void testPromptIDsNoLocalizer() {
        QuestionDef q = new QuestionDef();

        q.setTextID("long text id");
        if (!"long text id".equals(q.getTextID())) {
            fail("Long text ID getter/setter broken");
        }

        QuestionString hint = new QuestionString("hint");
        hint.setTextId("hint text id");
        q.putQuestionString("hint", hint);
        if (!"hint text id".equals(q.getQuestionString("hint").getTextId())) {
            fail("hint text ID getter/setter broken");
        }
    }

    public void testPromptsNoLocalizer() {
        QuestionDef q = new QuestionDef();

        q.putQuestionString("help", new QuestionString("help", "help text"));
        if (!"help text".equals(q.getQuestionString("help").getTextInner())) {
            fail("Help text getter/setter broken");
        }
    }
}
