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

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.test_utils.FormLoadingUtils;
import org.javarosa.xpath.IExprDataType;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.XPathUnhandledException;
import org.javarosa.xpath.XPathUnsupportedException;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.expr.XPathNumericLiteral;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.TreeElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author Phillip Mates
 */

public class XPathPathExprTest extends TestCase {

    private static final String formPath = new String("/test_xpathpathexpr.xml");

    public XPathPathExprTest(String name, TestMethod rTestMethod) {
        super(name, rTestMethod);
    }

    public XPathPathExprTest(String name) {
        super(name);
    }

    public XPathPathExprTest() {
        super();
    }

    public Test suite() {
        TestSuite aSuite = new TestSuite();

        aSuite.addTest(new XPathPathExprTest("XPath Path Expression Test", new TestMethod() {
            public void run(TestCase tc) {
                ((XPathPathExprTest)tc).doTests();
            }
        }));

        return aSuite;
    }

    public void doTests() {
        FormInstance instance = null;
        try {
            instance = FormLoadingUtils.loadFormInstance(formPath);
        } catch (IOException e) {
            fail("Unable to load form at " + formPath);
        } catch (InvalidStructureException e) {
            fail("Form at " + formPath + " has an invalid structure.");
        }

        // Used to reproduce bug where locations can't handle heterogeneous template paths.
        // This bug has been fixed and the following test now passes.
        testEval("/data/places/country[@id ='two']/state[@id = 'beehive_state']", instance, null, "Utah");
        testEval("/data/places/country[@id ='one']/name", instance, null, "Singapore");
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
}
