package org.javarosa.core.model.utils.test;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.test.DummyInstanceInitializationFactory;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.test_utils.ExprEvalUtils;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TreeUtilitiesTest {
    @Test
    public void fullCodeCoverageForTryBatchChildFetch() {
        FormParseInit fpi = new FormParseInit("/test_nested_multiplicities.xml");
        FormDef fd = fpi.getFormDef();

        ExprEvalUtils.testEval("count(/data/bikes/manufacturer[@american='yes'][count(model[.=1]) > 0]/model/@id)",
                fd.getInstance(), null, 4.0);
        ExprEvalUtils.testEval("count(/data/bikes/manufacturer[@american='yes'][count(model[.=1]) > '0']/model/@id)",
                fd.getInstance(), null, 4.0);
        ExprEvalUtils.testEval("/data/bikes/manufacturer/model[@id='long-haul']",
                fd.getInstance(), null, "0");
        ExprEvalUtils.testEval("count(/data/bikes/manufacturer[@american='yes'][count(model[.=1]) > /data/bikes/manufacturer/model[@id='long-haul']]/model/@id)",
                fd.getInstance(), null, 4.0);

    }
}
