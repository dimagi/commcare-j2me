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

package org.javarosa.xpath.test;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

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

import org.kxml2.io.KXmlParser;

/**
 *
 * @author Phillip Mates
 */

public class XPathPathExprTest extends TestCase {

    private static final String formName = new String("/test_xpathpathexpr.xml");

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
        EvaluationContext ec = new EvaluationContext(null);

        TreeElement root = new TreeElementParser(new KXmlParser(formName), 0, "instancename").parse();
        FormInstance instance = new FormInstance(root, "instancename");

        //Attribute XPath References
        //testEval("/@blah", null, null, new XPathUnsupportedException());
        //TODO: Need to test with model, probably in a different file

        String statesPath = "places/country/state";
        String wildcardIndex = "index/*";
        String indexOne = "index/some_index";
        String indexTwo = "index/another_index";
        XPathPathExpr expr = XPathReference.getPathExpr(wildcardIndex);
        XPathPathExpr expr2 = XPathReference.getPathExpr(indexOne);
        XPathPathExpr expr3 = XPathReference.getPathExpr(indexTwo);
        if (!expr.matches(expr2)) {
            fail("Bad Matching: " + wildcardIndex + " should match " + indexOne);
        }
        if (!expr2.matches(expr)) {
            fail("Bad Matching: " + indexOne + " should match " + wildcardIndex);
        }
        if (expr2.matches(expr3)) {
            fail("Bad Matching: " + indexOne + " should  not match " + indexTwo);
        }

        testEval("count(/data/strtest[@val = /data/string])", instance, null, new Double(1));
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
                    fail("Doubles outside of tolerance [" + o + "," + t + " ]");
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

    private FormInstance newDataModel() {
        return new FormInstance(new TreeElement());
    }

    private void addDataRef(FormInstance dm, String ref, IAnswerData data) {
        TreeReference treeRef = XPathReference.getPathExpr(ref).getReference(true);
        treeRef = inlinePositionArgs(treeRef);

        addNodeRef(dm, treeRef);


        if (data != null) {
            dm.resolveReference(treeRef).setValue(data);
        }
    }

    private TreeReference inlinePositionArgs(TreeReference treeRef) {
        //find/replace position predicates
        for (int i = 0; i < treeRef.size(); ++i) {
            Vector<XPathExpression> predicates = treeRef.getPredicate(i);
            if (predicates == null || predicates.size() == 0) {
                continue;
            }
            if (predicates.size() > 1) {
                throw new IllegalArgumentException("only position [] predicates allowed");
            }
            if (!(predicates.elementAt(0) instanceof XPathNumericLiteral)) {
                throw new IllegalArgumentException("only position [] predicates allowed");
            }
            double d = ((XPathNumericLiteral) predicates.elementAt(0)).d;
            if (d != (double) ((int) d)) {
                throw new IllegalArgumentException("invalid position: " + d);
            }

            int multiplicity = (int) d - 1;
            if (treeRef.getMultiplicity(i) != TreeReference.INDEX_UNBOUND) {
                throw new IllegalArgumentException("Cannot inline already qualified steps");
            }
            treeRef.setMultiplicity(i, multiplicity);
        }

        treeRef = treeRef.removePredicates();
        return treeRef;
    }

    private void addNodeRef(FormInstance dm, TreeReference treeRef) {
        TreeElement lastValidStep = dm.getRoot();
        for (int i = 1; i < treeRef.size(); ++i) {
            TreeElement step = dm.resolveReference(treeRef.getSubReference(i));
            if (step == null) {
                if (treeRef.getMultiplicity(i) == TreeReference.INDEX_ATTRIBUTE) {
                    //must be the last step
                    lastValidStep.setAttribute(null, treeRef.getName(i), "");
                    return;
                }
                String currentName = treeRef.getName(i);
                step = new TreeElement(currentName, treeRef.getMultiplicity(i) == TreeReference.INDEX_UNBOUND ? TreeReference.DEFAULT_MUTLIPLICITY : treeRef.getMultiplicity(i));
                lastValidStep.addChild(step);
            }
            lastValidStep = step;
        }
    }
}
