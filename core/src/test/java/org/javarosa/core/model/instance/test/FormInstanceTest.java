package org.javarosa.core.model.instance.test;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.test_utils.ExprEvalUtils;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class FormInstanceTest {
    private static final LivePrototypeFactory pf =  new LivePrototypeFactory();

    @Test
    public void testInstanceSerialization() {
        FormParseInit fpi = new FormParseInit("/xform_tests/test_repeat_insert_duplicate_triggering.xml");
        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        FormDef fd = fpi.getFormDef();
        // run initialization to ensure xforms-ready event and binds are
        // triggered.
        fd.initialize(true, new DummyInstanceInitializationFactory());

        FormInstance instance = fd.getMainInstance();
        FormInstance reSerializedInstance = reSerializeFormInstance(instance);

        assertTrue("Form instance root should be same after serialization",
                instance.getRoot().equals(reSerializedInstance.getRoot()));
    }

    private static FormInstance reSerializeFormInstance(FormInstance originalInstance) {
        FormInstance reSerializedInstance = null;
        try {
            reSerializedInstance = FormInstance.class.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            fail(e.getMessage());
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        try {
            originalInstance.writeExternal(out);
            out.flush();
            out.close();
        } catch (IOException e) {
            fail(e.getMessage());
        }
        try {
            reSerializedInstance.readExternal(new DataInputStream(new ByteArrayInputStream(baos.toByteArray())), pf);
        } catch (IOException | DeserializationException e) {
            fail(e.getMessage());
        }
        return reSerializedInstance;
    }

    @Test
    public void testFormEntryAfterSerialization() throws XPathSyntaxException {
        FormParseInit fpi = new FormParseInit("/xform_tests/test_repeat_insert_duplicate_triggering.xml");
        FormEntryController fec = fpi.getFormEntryController();
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        FormDef fd = fpi.getFormDef();
        // run initialization to ensure xforms-ready event and binds are
        // triggered.
        fd.initialize(true, new DummyInstanceInitializationFactory());

        FormInstance instance = fd.getMainInstance();
        do {
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
        EvaluationContext evalCtx = fd.getEvaluationContext();
        Date modified = (Date)ExprEvalUtils.xpathEval(evalCtx, "/data/how_many/@date_modified");


        FormInstance reSerializedInstance = reSerializeFormInstance(instance);

        fd.setInstance(reSerializedInstance);
        fd.initialize(true, new DummyInstanceInitializationFactory());
        FormEntryModel femodel = new FormEntryModel(fd);
        fec = new FormEntryController(femodel);
        fec.jumpToIndex(FormIndex.createBeginningOfFormIndex());

        do {
        } while (fec.stepToNextEvent() != FormEntryController.EVENT_END_OF_FORM);
        evalCtx = fd.getEvaluationContext();
        Date modified2 = (Date)ExprEvalUtils.xpathEval(evalCtx, "/data/how_many/@date_modified");
        assertTrue(modified.getTime() - modified2.getTime() < 3000);
    }
}
