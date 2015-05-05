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

package org.javarosa.core.model.instance.test;

import java.io.IOException;

import junit.framework.Test;
import org.javarosa.test.framework.AdaptedTestCase;
import junit.framework.TestSuite;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.test.framework.TestMethod;
import org.javarosa.test_utils.FormLoadingUtils;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

/**
 * DataInstance methods tests
 *
 * @author Phillip Mates
 */

public class DataInstanceTest extends AdaptedTestCase {

    private static final String formPath = new String("/test_xpathpathexpr.xml");

    public DataInstanceTest(String name, TestMethod rTestMethod) {
        super(name, rTestMethod);
    }

    public DataInstanceTest(String name) {
        super(name);
    }

    public DataInstanceTest() {
        super();
    }

    public static Test suite() {
        TestSuite aSuite = new TestSuite();

        aSuite.addTest(new DataInstanceTest("Data Instance Test", new TestMethod() {
            public void run(AdaptedTestCase tc) {
                ((DataInstanceTest)tc).doTests();
            }
        }));

        return aSuite;
    }

    public void doTests() {
        // load the xml doc into a form instance
        FormInstance model = null;
        try {
            model = FormLoadingUtils.loadFormInstance(formPath);
        } catch (IOException e) {
            fail("Unable to load form at " + formPath);
        } catch (InvalidStructureException e) {
            fail("Form at " + formPath + " has an invalid structure.");
        }

        EvaluationContext eval_ctx = new EvaluationContext(model);

        // make sure a valid path can be found even when the xml sub-elements
        // aren't homogeneous in structure
        assertTrue("Homogeneous template path for a reference",
                model.hasTemplatePath(exprToRef("/data/places/country[1]/name", eval_ctx)));

        assertTrue("Heterogeneous template path for a reference",
                model.hasTemplatePath(exprToRef("/data/places/country[1]/state[0]", eval_ctx)));

        assertTrue("Unfound template path for a reference",
                !model.hasTemplatePath(exprToRef("/data/places/fake[1]/name", eval_ctx)));

        assertTrue("Unfound template path for a reference",
                !model.hasTemplatePath(exprToRef("/data/places/country[1]/fake", eval_ctx)));
    }

    /**
     * Evaluate an xpath query expression into a reference.
     *
     * @param expr     xpath expression
     * @param eval_ctx contextual information needed to evaluate the expression
     */
    public TreeReference exprToRef(String expr, EvaluationContext eval_ctx) {
        XPathPathExpr xpe = null;
        try {
            xpe = (XPathPathExpr)XPathParseTool.parseXPath(expr);
        } catch (XPathSyntaxException xpse) {
        }

        if (xpe == null) {
            fail("Null expression or syntax error " + expr);
            return null;
        }

        TreeReference ref = null;
        try {
            TreeReference genericRef = xpe.getReference();
            if (genericRef.getContext() == TreeReference.CONTEXT_ORIGINAL) {
                ref = genericRef.contextualize(eval_ctx.getOriginalContext());
            } else {
                ref = genericRef.contextualize(eval_ctx.getContextRef());
            }
        } catch (XPathException xpex) {
            fail("Did not get expected exception type");
        }
        return ref;
    }
}
