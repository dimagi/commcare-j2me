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

package org.javarosa.xpath.expr.test;

import java.io.IOException;

import junit.framework.Test;
import org.javarosa.test.framework.AdaptedTestCase;
import junit.framework.TestSuite;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.test.framework.TestMethod;
import org.javarosa.test_utils.FormLoadingUtils;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

/**
 * @author Phillip Mates
 */

public class XPathPathExprTest extends AdaptedTestCase {

    public final static int NUM_TESTS = 2;

    public XPathPathExprTest(String name, TestMethod rTestMethod) {
        super(name, rTestMethod);
    }

    public XPathPathExprTest(String name) {
        super(name);
    }

    public XPathPathExprTest() {
        super();
    }

    public static Test suite() {
        TestSuite aSuite = new TestSuite();

        for (int i = 1; i <= NUM_TESTS; i++) {
            final int testID = i;
            aSuite.addTest(new XPathPathExprTest("XPath Path Expression Test", new TestMethod() {
                public void run(AdaptedTestCase tc) {
                    ((XPathPathExprTest)tc).doTest(testID);
                }
            }));
        }

        return aSuite;
    }

    public void doTest(int i) {
        switch (i) {
            case 1:
                testHeterogeneousPaths();
                break;
            case 2:
                testNestedMultiplicities();
                break;
        }
    }

    private void testHeterogeneousPaths() {
        FormInstance instance = loadInstance("/test_xpathpathexpr.xml");

        // Used to reproduce bug where locations can't handle heterogeneous template paths.
        // This bug has been fixed and the following test now passes.
        testEval("/data/places/country[@id ='two']/state[@id = 'beehive_state']", instance, null, "Utah");
        testEval("/data/places/country[@id ='one']/name", instance, null, "Singapore");
    }

    /**
     * Some simple xpath expressions with multiple predicates that operate over
     * nodesets.
     */
    private void testNestedMultiplicities() {
        FormParseInit fpi = new FormParseInit("/test_nested_multiplicities.xml");
        FormDef fd = fpi.getFormDef();
        FormEntryModel fem = fpi.getFormEntryModel();

        testEval("/data/bikes/manufacturer/model[@id='pista']/@color",
                fd.getInstance(), null, "seafoam");
        testEval("join(' ', /data/bikes/manufacturer[@american='yes']/model[.=1]/@id)",
                fd.getInstance(), null, "karate-monkey vamoots");
        testEval("count(/data/bikes/manufacturer[@american='yes'][count(model[.=1]) > 0]/model/@id)",
                fd.getInstance(), null, 4.0);
        testEval("join(' ', /data/bikes/manufacturer[@american='yes'][count(model[.=1]) > 0]/model/@id)",
                fd.getInstance(), null, "karate-monkey long-haul cross-check vamoots");
        testEval("join(' ', /data/bikes/manufacturer[@american='yes'][count(model=1) > 0]/model/@id)",
                fd.getInstance(), null, new XPathTypeMismatchException());
        testEval("join(' ', /data/bikes/manufacturer[@american='no'][model=1]/model/@id)",
                fd.getInstance(), null, new XPathTypeMismatchException());
    }

    private void testEval(String expr, FormInstance model, EvaluationContext ec, Object expected) {
        testEval(expr, model, ec, expected, 1.0e-12);
    }

    private void testEval(String expr, FormInstance model, EvaluationContext ec, Object expected, double tolerance) {
        XPathExpression xpe = null;
        boolean exceptionExpected = (expected instanceof XPathException);

        if (ec == null) {
            ec = new EvaluationContext(model);
        }

        try {
            xpe = XPathParseTool.parseXPath(expr);
        } catch (XPathSyntaxException xpse) {
        }

        if (xpe == null) {
            fail("Null expression or syntax error " + expr);
        }

        try {
            Object result = XPathFuncExpr.unpack(xpe.eval(model, ec));
            if (tolerance != XPathFuncExpr.DOUBLE_TOLERANCE) {
                System.out.println(expr + " = " + result);
            }

            if (exceptionExpected) {
                fail("Expected exception, expression : " + expr);
            } else if ((result instanceof Double && expected instanceof Double)) {
                Double o = ((Double)result).doubleValue();
                Double t = ((Double)expected).doubleValue();
                if (Math.abs(o - t) > tolerance) {
                    fail("Doubles outside of tolerance: got " + o + ", expected " + t);
                }
            } else if (!expected.equals(result)) {
                fail("Expected " + expected + ", got " + result);
            }
        } catch (XPathException xpex) {
            if (!exceptionExpected) {
                fail("Did not expect " + xpex.getClass() + " exception");
            } else if (xpex.getClass() != expected.getClass()) {
                fail("Did not get expected exception type");
            }
        }
    }

    /**
     * Load a form instance from a path.
     * Doesn't create a model or main instance.
     *
     * @param formPath path of the form to load, relative to project build
     * @return FormInstance created from the path pointed to, or null if any
     * error occurs.
     */
    private FormInstance loadInstance(String formPath) {
        FormInstance instance = null;
        try {
            instance = FormLoadingUtils.loadFormInstance(formPath);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unable to load form at " + formPath);
        } catch (InvalidStructureException e) {
            e.printStackTrace();
            fail("Form at " + formPath + " has an invalid structure.");
        } catch(Throwable e) {
            e.printStackTrace();
        }
        return instance;
    }
}
