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
import org.javarosa.xml.TreeElementParser;
import org.javarosa.xml.util.InvalidStructureException;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 *
 * @author Phillip Mates
 */

public class XPathPathExprTest extends TestCase {

    private static final String formPath = new String("/test_xpathpathexpr.xml");
    // private static final String formPath = new String("/test_small_xml_example.xml");

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
                ((XPathPathExprTest) tc).doTests();
            }
        }));

        return aSuite;
    }

    public void doTests() {
        KXmlParser parser = null;
        TreeElement root = null;
        EvaluationContext ec = new EvaluationContext(null);

        // read in xml
        try {
            InputStream is = System.class.getResourceAsStream(formPath);
            parser = new KXmlParser();
            parser.setInput(is, "UTF-8");
        } catch (XmlPullParserException e) {
            fail("Contents at filepath could not be parsed as XML: " + formPath);
            return;
        }

        // turn parsed xml into a form instance
        try {
            root = new TreeElementParser(parser, 0, "data").parse();
        } catch (Exception e) {
            fail("File couldn't be parsed into a TreeElement: " + formPath);
            return;
        }
        FormInstance instance = null;
        try {
            instance = new FormInstance(root.getChildAt(0), "data");
        } catch (Exception e) {
            fail("couldn't create form instance");
        }

        testEval("count(/data/places/country/state)", instance, null, new Double(2));
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
                Double o = ((Double) result).doubleValue();
                Double t = ((Double) expected).doubleValue();
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
