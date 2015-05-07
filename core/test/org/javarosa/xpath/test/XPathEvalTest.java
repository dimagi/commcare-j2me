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
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.XPathUnhandledException;
import org.javarosa.xpath.XPathUnsupportedException;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.expr.XPathNumericLiteral;
import org.javarosa.xpath.expr.XPathPathExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathEvalTest extends TestCase {
    public XPathEvalTest(String name, TestMethod rTestMethod) {
        super(name, rTestMethod);
    }

    public XPathEvalTest(String name) {
        super(name);
    }

    public XPathEvalTest() {
        super();
    }

    public Test suite() {
        TestSuite aSuite = new TestSuite();

        aSuite.addTest(new XPathEvalTest("XPath Evaluation Test", new TestMethod() {
            public void run(TestCase tc) {
                ((XPathEvalTest)tc).doTests();
            }
        }));

        return aSuite;
    }

    private void testEval(String expr, FormInstance model, EvaluationContext ec, Object expected) {
        testEval(expr, model, ec, expected, 1.0e-12);
    }

    private void testEval(String expr, FormInstance model, EvaluationContext ec, Object expected, double tolerance) {
        //System.out.println("[" + expr + "]");

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
                    fail("Doubles outside of tolerance [" + o + "," + t + " ]");
                }
            } else if (!expected.equals(result)) {
                fail("Expected " + expected + ", got " + result);
            }
        } catch (XPathException xpex) {
            if (!exceptionExpected) {
                fail("Did not expect " + xpex.getClass() + " exception");
            } else if (xpex.getClass() != expected.getClass()) {
                fail("Expected " + expected.getClass() +
                        "exception type but was provided" + xpex.getClass());
            }
        }
    }

    public void doTests() {
        EvaluationContext ec = getFunctionHandlers();

        FormInstance instance = createTestInstance();

        /* unsupporteds */
        testEval("/union | /expr", null, null, new XPathUnsupportedException());
        testEval("/descendant::blah", null, null, new XPathUnsupportedException());
        testEval("/cant//support", null, null, new XPathUnsupportedException());
        testEval("/text()", null, null, new XPathUnsupportedException());
        testEval("/namespace:*", null, null, new XPathUnsupportedException());
        testEval("(filter-expr)[5]", instance, null, new XPathUnsupportedException());
        testEval("(filter-expr)/data", instance, null, new XPathUnsupportedException());
        /* numeric literals */
        testEval("5", null, null, new Double(5.0));
        testEval("555555.555", null, null, new Double(555555.555));
        testEval(".000555", null, null, new Double(0.000555));
        testEval("0", null, null, new Double(0.0));
        testEval("-5", null, null, new Double(-5.0));
        testEval("-0", null, null, new Double(-0.0));
        testEval("1230000000000000000000", null, null, new Double(1.23e21));
        testEval("0.00000000000000000123", null, null, new Double(1.23e-18));
        /* string literals */
        testEval("''", null, null, "");
        testEval("'\"'", null, null, "\"");
        testEval("\"test string\"", null, null, "test string");
        testEval("'   '", null, null, "   ");
        /* base type conversion functions */
        testEval("true()", null, null, Boolean.TRUE);
        testEval("false()", null, null, Boolean.FALSE);
        testEval("boolean(true())", null, null, Boolean.TRUE);
        testEval("boolean(false())", null, null, Boolean.FALSE);
        testEval("boolean(1)", null, null, Boolean.TRUE);
        testEval("boolean(-1)", null, null, Boolean.TRUE);
        testEval("boolean(0.0001)", null, null, Boolean.TRUE);
        testEval("boolean(0)", null, null, Boolean.FALSE);
        testEval("boolean(-0)", null, null, Boolean.FALSE);
        testEval("boolean(number('NaN'))", null, null, Boolean.FALSE);
        testEval("boolean(1 div 0)", null, null, Boolean.TRUE);
        testEval("boolean(-1 div 0)", null, null, Boolean.TRUE);
        testEval("boolean('')", null, null, Boolean.FALSE);
        testEval("boolean('asdf')", null, null, Boolean.TRUE);
        testEval("boolean('  ')", null, null, Boolean.TRUE);
        testEval("boolean('false')", null, null, Boolean.TRUE);
        testEval("boolean(date('2000-01-01'))", null, null, Boolean.TRUE);
        testEval("boolean(convertible())", null, ec, Boolean.TRUE);
        testEval("boolean(inconvertible())", null, ec, new XPathTypeMismatchException());
        testEval("number(true())", null, null, new Double(1.0));
        testEval("number(false())", null, null, new Double(0.0));
        testEval("number('100')", null, null, new Double(100.0));
        testEval("number('100.001')", null, null, new Double(100.001));
        testEval("number('.1001')", null, null, new Double(0.1001));
        testEval("number('1230000000000000000000')", null, null, new Double(1.23e21));
        testEval("number('0.00000000000000000123')", null, null, new Double(1.23e-18));
        testEval("number('0')", null, null, new Double(0.0));
        testEval("number('-0')", null, null, new Double(-0.0));
        testEval("number(' -12345.6789  ')", null, null, new Double(-12345.6789));
        testEval("number('NaN')", null, null, new Double(Double.NaN));
        testEval("number('not a number')", null, null, new Double(Double.NaN));
        testEval("number('- 17')", null, null, new Double(Double.NaN));
        testEval("number('  ')", null, null, new Double(Double.NaN));
        testEval("number('')", null, null, new Double(Double.NaN));
        testEval("number('Infinity')", null, null, new Double(Double.NaN));
        testEval("number('1.1e6')", null, null, new Double(Double.NaN));
        testEval("number('34.56.7')", null, null, new Double(Double.NaN));
        testEval("number(10)", null, null, new Double(10.0));
        testEval("number(0)", null, null, new Double(0.0));
        testEval("number(-0)", null, null, new Double(-0.0));
        testEval("number(-123.5)", null, null, new Double(-123.5));
        testEval("number(number('NaN'))", null, null, new Double(Double.NaN));
        testEval("number(1 div 0)", null, null, new Double(Double.POSITIVE_INFINITY));
        testEval("number(-1 div 0)", null, null, new Double(Double.NEGATIVE_INFINITY));
        testEval("number(date('1970-01-01'))", null, null, new Double(0.0));
        testEval("number(date('1970-01-02'))", null, null, new Double(1.0));
        testEval("number(date('1969-12-31'))", null, null, new Double(-1.0));
        testEval("number(date('2008-09-05'))", null, null, new Double(14127.0));
        testEval("number(date('1941-12-07'))", null, null, new Double(-10252.0));
        testEval("number(convertible())", null, ec, new Double(5.0));
        testEval("number(inconvertible())", null, ec, new XPathTypeMismatchException());
        testEval("string(true())", null, null, "true");
        testEval("string(false())", null, null, "false");
        testEval("string(number('NaN'))", null, null, "NaN");
        testEval("string(1 div 0)", null, null, "Infinity");
        testEval("string(-1 div 0)", null, null, "-Infinity");
        testEval("string(0)", null, null, "0");
        testEval("string(-0)", null, null, "0");
        testEval("string(123456.0000)", null, null, "123456");
        testEval("string(-123456)", null, null, "-123456");
        testEval("string(1)", null, null, "1");
        testEval("string(-1)", null, null, "-1");
        testEval("string(.557586)", null, null, "0.557586");
        //broken: testEval("string(1230000000000000000000)", null, null, "1230000000000000000000");
        //broken: testEval("string(0.00000000000000000123)", null, null, "0.00000000000000000123");
        testEval("string('')", null, null, "");
        testEval("string('  ')", null, null, "  ");
        testEval("string('a string')", null, null, "a string");
        testEval("string(date('1989-11-09'))", null, null, "1989-11-09");
        testEval("string(convertible())", null, ec, "hi");
        testEval("string(inconvertible())", null, ec, new XPathTypeMismatchException());
        testEval("substr('hello',0)", null, null, "hello");
        testEval("substr('hello',0,5)", null, null, "hello");
        testEval("substr('hello',1)", null, null, "ello");
        testEval("substr('hello',1,5)", null, null, "ello");
        testEval("substr('hello',1,4)", null, null, "ell");
        testEval("substr('hello',-2)", null, null, "lo");
        testEval("substr('hello',0,-1)", null, null, "hell");
        testEval("substr('',0,1)", null, null, "");
        testEval("substr('hello',0,8)", null, null, "");
        testEval("date('2000-01-01')", null, null, DateUtils.getDate(2000, 1, 1));
        testEval("date('1945-04-26')", null, null, DateUtils.getDate(1945, 4, 26));
        testEval("date('1996-02-29')", null, null, DateUtils.getDate(1996, 2, 29));
        testEval("date('1983-09-31')", null, null, new XPathTypeMismatchException());
        testEval("date('not a date')", null, null, new XPathTypeMismatchException());
        testEval("date(0)", null, null, DateUtils.getDate(1970, 1, 1));
        testEval("date(6.5)", null, null, DateUtils.getDate(1970, 1, 7));
        testEval("date(1)", null, null, DateUtils.getDate(1970, 1, 2));
        testEval("date(-1)", null, null, DateUtils.getDate(1969, 12, 31));
        testEval("date(14127)", null, null, DateUtils.getDate(2008, 9, 5));
        testEval("date(-10252)", null, null, DateUtils.getDate(1941, 12, 7));
        testEval("date(date('1989-11-09'))", null, null, DateUtils.getDate(1989, 11, 9));
        testEval("date(true())", null, null, new XPathTypeMismatchException());
        testEval("date(convertible())", null, ec, new XPathTypeMismatchException());
        //note: there are lots of time and timezone-like issues with dates that should be tested (particularly DST changes),
        //    but it's just too hard and client-dependent, so not doing it now
        //  basically:
        //        dates cannot reliably be compared/used across time zones (an issue with the code)
        //        any time-of-day or DST should be ignored when comparing/using a date (an issue with testing)
        /* other built-in functions */
        testEval("not(true())", null, null, Boolean.FALSE);
        testEval("not(false())", null, null, Boolean.TRUE);
        testEval("not('')", null, null, Boolean.TRUE);
        testEval("boolean-from-string('true')", null, null, Boolean.TRUE);
        testEval("boolean-from-string('false')", null, null, Boolean.FALSE);
        testEval("boolean-from-string('whatever')", null, null, Boolean.FALSE);
        testEval("boolean-from-string('1')", null, null, Boolean.TRUE);
        testEval("boolean-from-string('0')", null, null, Boolean.FALSE);
        testEval("boolean-from-string(1)", null, null, Boolean.TRUE);
        testEval("boolean-from-string(1.0)", null, null, Boolean.TRUE);
        testEval("boolean-from-string(1.0001)", null, null, Boolean.FALSE);
        testEval("boolean-from-string(true())", null, null, Boolean.TRUE);
        testEval("if(true(), 5, 'abc')", null, null, new Double(5.0));
        testEval("if(false(), 5, 'abc')", null, null, "abc");
        testEval("if(6 > 7, 5, 'abc')", null, null, "abc");
        testEval("if('', 5, 'abc')", null, null, "abc");
        testEval("selected('apple baby crimson', 'apple')", null, null, Boolean.TRUE);
        testEval("selected('apple baby crimson', 'baby')", null, null, Boolean.TRUE);
        testEval("selected('apple baby crimson', 'crimson')", null, null, Boolean.TRUE);
        testEval("selected('apple baby crimson', '  baby  ')", null, null, Boolean.TRUE);
        testEval("selected('apple baby crimson', 'babby')", null, null, Boolean.FALSE);
        testEval("selected('apple baby crimson', 'bab')", null, null, Boolean.FALSE);
        testEval("selected('apple', 'apple')", null, null, Boolean.TRUE);
        testEval("selected('apple', 'ovoid')", null, null, Boolean.FALSE);
        testEval("selected('', 'apple')", null, null, Boolean.FALSE);
        /* operators */

        testEval("min(5.5, 0.5)", null, null, new Double(0.5));
        testEval("min(5.5)", null, null, new Double(5.5));
        testEval("date(min(date('2012-02-05'), date('2012-01-01')))", null, null, DateUtils.parseDate("2012-01-01"));

        testEval("5.5 + 5.5", null, null, new Double(11.0));
        testEval("0 + 0", null, null, new Double(0.0));
        testEval("6.1 - 7.8", null, null, new Double(-1.7));
        testEval("-3 + 4", null, null, new Double(1.0));
        testEval("3 + -4", null, null, new Double(-1.0));
        testEval("1 - 2 - 3", null, null, new Double(-4.0));
        testEval("1 - (2 - 3)", null, null, new Double(2.0));
        testEval("-(8*5)", null, null, new Double(-40.0));
        testEval("-'19'", null, null, new Double(-19.0));
        testEval("1.1 * -1.1", null, null, new Double(-1.21));
        testEval("-10 div -4", null, null, new Double(2.5));
        testEval("2 * 3 div 8 * 2", null, null, new Double(1.5));
        testEval("3 + 3 * 3", null, null, new Double(12.0));
        testEval("1 div 0", null, null, new Double(Double.POSITIVE_INFINITY));
        testEval("-1 div 0", null, null, new Double(Double.NEGATIVE_INFINITY));
        testEval("0 div 0", null, null, new Double(Double.NaN));
        testEval("3.1 mod 3.1", null, null, new Double(0.0));
        testEval("5 mod 3.1", null, null, new Double(1.9));
        testEval("2 mod 3.1", null, null, new Double(2.0));
        testEval("0 mod 3.1", null, null, new Double(0.0));
        testEval("5 mod -3", null, null, new Double(2.0));
        testEval("-5 mod 3", null, null, new Double(-2.0));
        testEval("-5 mod -3", null, null, new Double(-2.0));
        testEval("5 mod 0", null, null, new Double(Double.NaN));
        testEval("5 * (6 + 7)", null, null, new Double(65.0));
        testEval("'123' * '456'", null, null, new Double(56088.0));
        testEval("true() + 8", null, null, new Double(9.0));
        testEval("date('2008-09-08') - date('1983-10-06')", null, null, new Double(9104.0));
        testEval("true() and true()", null, null, Boolean.TRUE);
        testEval("true() and false()", null, null, Boolean.FALSE);
        testEval("false() and false()", null, null, Boolean.FALSE);
        testEval("true() or true()", null, null, Boolean.TRUE);
        testEval("true() or false()", null, null, Boolean.TRUE);
        testEval("false() or false()", null, null, Boolean.FALSE);
        testEval("true() or true() and false()", null, null, Boolean.TRUE);
        testEval("(true() or true()) and false()", null, null, Boolean.FALSE);
        testEval("true() or date('')", null, null, Boolean.TRUE); //short-circuiting
        testEval("false() and date('')", null, null, Boolean.FALSE); //short-circuiting
        testEval("'' or 17", null, null, Boolean.TRUE);
        testEval("false() or 0 + 2", null, null, Boolean.TRUE);
        testEval("(false() or 0) + 2", null, null, new Double(2.0));
        testEval("4 < 5", null, null, Boolean.TRUE);
        testEval("5 < 5", null, null, Boolean.FALSE);
        testEval("6 < 5", null, null, Boolean.FALSE);
        testEval("4 <= 5", null, null, Boolean.TRUE);
        testEval("5 <= 5", null, null, Boolean.TRUE);
        testEval("6 <= 5", null, null, Boolean.FALSE);
        testEval("4 > 5", null, null, Boolean.FALSE);
        testEval("5 > 5", null, null, Boolean.FALSE);
        testEval("6 > 5", null, null, Boolean.TRUE);
        testEval("4 >= 5", null, null, Boolean.FALSE);
        testEval("5 >= 5", null, null, Boolean.TRUE);
        testEval("6 >= 5", null, null, Boolean.TRUE);
        testEval("-3 > -6", null, null, Boolean.TRUE);
        testEval("true() > 0.9999", null, null, Boolean.TRUE);
        testEval("'-17' > '-172'", null, null, Boolean.TRUE); //no string comparison: converted to number
        testEval("'abc' < 'abcd'", null, null, Boolean.FALSE); //no string comparison: converted to NaN
        testEval("date('2001-12-26') > date('2001-12-25')", null, null, Boolean.TRUE);
        testEval("date('1969-07-20') < date('1969-07-21')", null, null, Boolean.TRUE);

        testEval("double(date('2004-05-01T05:00:00')) > double(date('2004-05-01T02:00:00'))", null, null, Boolean.TRUE);
        testEval("not(double(date('2004-05-01T05:00:00')) < double(date('2004-05-01T02:00:00')))", null, null, Boolean.TRUE);
        testEval("not(double(date('2004-05-01T05:00:00')) = double(date('2004-05-01T02:00:00')))", null, null, Boolean.TRUE);
        testEval("double(date('2004-05-01T04:00:00')) < double(date('2004-05-01T016:00:00'))", null, null, Boolean.TRUE);

        testEval("(double(date('2004-05-01T07:00:00')) - double(date('2004-05-01T03:00:00'))) < (6.0 div 24) ", null, null, Boolean.TRUE);
        testEval("(double(date('2004-05-01T07:00:00')) - double(date('2004-05-01T00:30:00'))) > (6.0 div 24) ", null, null, Boolean.TRUE);
        testEval("(double(date('2004-05-03T07:00:00')) - double(date('2004-05-01T03:00:00'))) > (6.0 div 24) ", null, null, Boolean.TRUE);

        testEval("abs(-3.5)", null, null, new Double(3.5));
        testEval("abs(2)", null, null, new Double(2.0));
        testEval("floor(-4.8)", null, null, new Double(-5.0));
        testEval("floor(100.2)", null, null, new Double(100.0));
        testEval("ceiling(-0.5)", null, null, new Double(0.0));
        testEval("ceiling(10.4)", null, null, new Double(11.0));
        testEval("round(1.5)", null, null, new Double(2.0));
        testEval("round(-1.5)", null, null, new Double(-1.0));
        testEval("round(1.455)", null, null, new Double(1.0));

        testEval("log(" + Math.E + ")", null, null, new Double(1.0));
        testEval("log(1)", null, null, new Double(0.0));
        testEval("log10(100)", null, null, new Double(2.0));
        testEval("log10(1)", null, null, new Double(0.0));


        testEval("pow(2, 2)", null, null, new Double(4.0));
        testEval("pow(2, 0)", null, null, new Double(1.0));
        testEval("pow(0, 4)", null, null, new Double(0.0));
        testEval("pow(2.5, 2)", null, null, new Double(6.25));
        testEval("pow(0.5, 2)", null, null, new Double(.25));

        testEval("pow(-1, 2)", null, null, new Double(1.0));
        testEval("pow(-1, 3)", null, null, new Double(-1.0));

        //So raising things to decimal powers is.... very hard
        //to evaluated exactly due to double floating point
        //precision. We'll try for things with clean answers
        //testEval("pow(4, 0.5)", null, null, new Double(2.0), .001);
        //testEval("pow(16, 0.25)", null, null, new Double(2.0), .001);
        //CTS: We're going to skip trying to do any sort of hackery workaround
        //for this for now and go with "Integer powers only"

        testEval("false() and false() < true()", null, null, Boolean.FALSE);
        testEval("(false() and false()) < true()", null, null, Boolean.TRUE);
        testEval("6 < 7 - 4", null, null, Boolean.FALSE);
        testEval("(6 < 7) - 4", null, null, new Double(-3.0));
        testEval("3 < 4 < 5", null, null, Boolean.TRUE);
        testEval("3 < (4 < 5)", null, null, Boolean.FALSE);
        testEval("true() = true()", null, null, Boolean.TRUE);
        testEval("true() = false()", null, null, Boolean.FALSE);
        testEval("true() != true()", null, null, Boolean.FALSE);
        testEval("true() != false()", null, null, Boolean.TRUE);
        testEval("3 = 3", null, null, Boolean.TRUE);
        testEval("3 = 4", null, null, Boolean.FALSE);
        testEval("3 != 3", null, null, Boolean.FALSE);
        testEval("3 != 4", null, null, Boolean.TRUE);
        testEval("6.1 - 7.8 = -1.7", null, null, Boolean.TRUE); //handle floating point rounding
        testEval("'abc' = 'abc'", null, null, Boolean.TRUE);
        testEval("'abc' = 'def'", null, null, Boolean.FALSE);
        testEval("'abc' != 'abc'", null, null, Boolean.FALSE);
        testEval("'abc' != 'def'", null, null, Boolean.TRUE);
        testEval("'' = ''", null, null, Boolean.TRUE);
        testEval("true() = 17", null, null, Boolean.TRUE);
        testEval("0 = false()", null, null, Boolean.TRUE);
        testEval("true() = 'true'", null, null, Boolean.TRUE);
        testEval("17 = '17.0000000'", null, null, Boolean.TRUE);
        testEval("'0017.' = 17", null, null, Boolean.TRUE);
        testEval("'017.' = '17.000'", null, null, Boolean.FALSE);
        testEval("date('2004-05-01') = date('2004-05-01')", null, null, Boolean.TRUE);
        testEval("true() != date('1999-09-09')", null, null, Boolean.FALSE);
        testEval("false() and true() != true()", null, null, Boolean.FALSE);
        testEval("(false() and true()) != true()", null, null, Boolean.TRUE);
        testEval("-3 < 3 = 6 >= 6", null, null, Boolean.TRUE);
        /* functions, including custom function handlers */
        testEval("weighted-checklist(5)", null, null, new XPathArityException());
        testEval("weighted-checklist(5, 5, 5)", null, null, new XPathArityException());
        testEval("substr('hello')", null, null, new XPathArityException());
        testEval("join()", null, null, new XPathArityException());
        testEval("max()", null, null, new XPathArityException());
        testEval("min()", null, null, new XPathArityException());
        testEval("true(5)", null, null, new XPathArityException());
        testEval("number()", null, null, new XPathArityException());
        testEval("string('too', 'many', 'args')", null, null, new XPathArityException());
        testEval("not-a-function()", null, null, new XPathUnhandledException());
        testEval("testfunc()", null, ec, Boolean.TRUE);
        testEval("add(3, 5)", null, ec, new Double(8.0));
        testEval("add('17', '-14')", null, ec, new Double(3.0));
        // proto not setup for 0 arguments
        testEval("proto()", null, ec, new XPathArityException());
        testEval("proto(5, 5)", null, ec, "[Double:5.0,Double:5.0]");
        testEval("proto(6)", null, ec, "[Double:6.0]");
        testEval("proto('asdf')", null, ec, "[Double:NaN]");
        testEval("proto('7', '7')", null, ec, "[Double:7.0,Double:7.0]"); //note: args treated as doubles because
        //(double, double) prototype takes precedence and strings are convertible to doubles
        testEval("proto(1.1, 'asdf', true())", null, ec, "[Double:1.1,String:asdf,Boolean:true]");
        testEval("proto(false(), false(), false())", null, ec, "[Double:0.0,String:false,Boolean:false]");
        testEval("proto(1.1, 'asdf', inconvertible())", null, ec, new XPathTypeMismatchException());

        // proto not setup for 4 arguments
        testEval("proto(1.1, 'asdf', true(), 16)", null, ec, new XPathArityException());

        testEval("raw()", null, ec, "[]");
        testEval("raw(5, 5)", null, ec, "[Double:5.0,Double:5.0]");
        testEval("raw('7', '7')", null, ec, "[String:7,String:7]");
        testEval("raw('1.1', 'asdf', 17)", null, ec, "[Double:1.1,String:asdf,Boolean:true]"); //convertible to prototype
        testEval("raw(get-custom(false()), get-custom(true()))", null, ec, "[CustomType:,CustomSubType:]");
        testEval("concat()", null, ec, "");
        testEval("concat('a')", null, ec, "a");
        testEval("concat('a','b','')", null, ec, "ab");
        testEval("concat('ab','cde','','fgh',1,false(),'ijklmnop')", null, ec, "abcdefgh1falseijklmnop");
        testEval("check-types(55, '55', false(), '1999-09-09', get-custom(false()))", null, ec, Boolean.TRUE);
        testEval("check-types(55, '55', false(), '1999-09-09', get-custom(true()))", null, ec, Boolean.TRUE);
        testEval("regex('12345','[0-9]+')", null, ec, Boolean.TRUE);
        testEval("upper-case('SimpLY')", null, null, new String("SIMPLY"));
        testEval("lower-case('rEd')", null, null, new String("red"));
        testEval("contains('', 'stuff')", null, null, Boolean.FALSE);
        testEval("contains('stuff', '')", null, null, Boolean.TRUE);
        testEval("contains('know', 'now')", null, null, Boolean.TRUE);
        testEval("contains('now', 'know')", null, null, Boolean.FALSE);
        testEval("starts-with('finish', 'fin')", null, null, Boolean.TRUE);
        testEval("starts-with('keep', '')", null, null, Boolean.TRUE);
        testEval("starts-with('why', 'y')", null, null, Boolean.FALSE);
        testEval("ends-with('elements', 'nts')", null, null, Boolean.TRUE);
        testEval("ends-with('elements', 'xenon')", null, null, Boolean.FALSE);
        testEval("translate('aBcdE', 'xyz', 'qrs')", null, null, new String("aBcdE"));
        testEval("translate('bosco', 'bos', 'sfo')", null, null, new String("sfocf"));
        testEval("translate('ramp', 'mapp', 'nbqr')", null, null, new String("rbnq"));
        testEval("translate('yellow', 'low', 'or')", null, null, new String("yeoor"));
        testEval("translate('bora bora', 'a', 'bc')", null, null, new String("borb borb"));
        testEval("translate('squash me', 'aeiou ', '')", null, null, new String("sqshm"));
        testEval("regex('aaaabfooaaabgarplyaaabwackyb', 'a*b')", null, null, Boolean.TRUE);
        testEval("regex('photo', 'a*b')", null, null, Boolean.FALSE);
        testEval("replace('aaaabfooaaabgarplyaaabwackyb', 'a*b', '-')", null, null, new String("-foo-garply-wacky-"));
        testEval("replace('abbc', 'a(.*)c', '$1')", null, null, new String("$1"));
        testEval("replace('aaabb', '[ab][ab][ab]', '')", null, null, new String("bb"));
        //Variables
        EvaluationContext varContext = getVariableContext();
        testEval("$var_float_five", null, varContext, new Double(5.0));
        testEval("$var_string_five", null, varContext, "five");
        testEval("$var_int_five", null, varContext, new Double(5.0));
        testEval("$var_double_five", null, varContext, new Double(5.0));

        //Attribute XPath References
        //testEval("/@blah", null, null, new XPathUnsupportedException());
        //TODO: Need to test with model, probably in a different file

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


        try {
            testEval("null-proto()", null, ec, new XPathUnhandledException());
            fail("Did not get expected null pointer");
        } catch (NullPointerException npe) {
            //expected
        }

        ec.addFunctionHandler(read);
        ec.addFunctionHandler(write);

        read.val = "testing-read";
        testEval("read()", null, ec, "testing-read");

        testEval("write('testing-write')", null, ec, Boolean.TRUE);
        if (!"testing-write".equals(write.val)) {
            fail("Custom function handler did not successfully send data to external source");
        }

        addDataRef(instance, "/data/string", new StringData("string"));
        addDataRef(instance, "/data/int", new IntegerData(17));
        addDataRef(instance, "/data/int_two", new IntegerData(5));
        addDataRef(instance, "/data/string_two", new StringData("2"));
        addDataRef(instance, "/data/predtest[1]/@val", new StringData("2.0"));
        addDataRef(instance, "/data/predtest[2]/@val", new StringData("2"));
        addDataRef(instance, "/data/predtest[3]/@val", new StringData("string"));

        addDataRef(instance, "/data/strtest[1]/@val", new StringData("a"));
        addDataRef(instance, "/data/strtest[2]/@val", new StringData("b"));
        addDataRef(instance, "/data/strtest[3]/@val", new StringData("string"));

        testEval("/data/string", instance, null, "string");
        testEval("/data/int", instance, null, new Double(17.0));

        testEval("min(/data/int, /data/int_two)", instance, null, new Double(5.0));

        testEval("count(/data/predtest[@val = /data/string_two])", instance, null, new Double(2));
        testEval("count(/data/predtest[@val = 2])", instance, null, new Double(2));
        testEval("count(/data/predtest[2 = @val])", instance, null, new Double(2));

        testEval("count(/data/strtest[@val = 'a'])", instance, null, new Double(1));
        testEval("count(/data/strtest[@val = 2])", instance, null, new Double(0));
        testEval("count(/data/strtest[@val = /data/string])", instance, null, new Double(1));





        /* fetching from model */
//        testEval("/", dm1, null, "");
//        testEval("/non-existent", dm1, null, "");
//
//        addDataRef(dm1, "/data/test", null);
//        addNodeRef(dm1, "/empty-group", false);
//        testEval("/", dm1, null, "");
//        testEval("/data", dm1, null, "");
//        testEval("/empty-group", dm1, null, "");
//        testEval("/empty-group/undefined", dm1, null, "");
//        testEval("/data/test", dm1, null, "");
//        testEval("/data/test/too-deep", dm1, null, "");
//

//        addDataRef(dm1, "/data/date", new DateData(DateUtils.getDate(2006, 6, 13)));
//
//        SelectOneData sod = new SelectOneData(new Selection(1, getSelectQuestion(false)));
//        QuestionDef smq = getSelectQuestion(true);
//        Vector sv = new Vector();
//        sv.addElement(new Selection(0, smq));
//        sv.addElement(new Selection(3, smq));
//        addDataRef(dm1, "/data/select-one", sod);
//        addDataRef(dm1, "/data/select-multi", new SelectMultiData(sv));
//        addDataRef(dm1, "/data/custom", new CustomAnswerData());
//
//        testEval("/data/string", dm1, null, "string");
//        testEval("/data/int", dm1, null, new Double(17.0));
//        testEval("/data/date", dm1, null, DateUtils.getDate(2006, 6, 13));
//        testEval("/data/select-one", dm1, null, "val2");
//        testEval("/data/select-multi", dm1, null, "val1 val4");
//        testEval("/data/custom", dm1, null, new CustomType());
//        testEval("selected(/data/select-multi, 'val4')", dm1, null, Boolean.TRUE);
//        testEval("selected(/data/select-multi, 'val3')", dm1, null, Boolean.FALSE);
//        testEval("/data/child::int + /child::data/int", dm1, null, new Double(34.0));
//        testEval("concat(/data/string, /data/date, add(/data/int, /data/int))", dm1, ec, "string2006-06-1334");
//        //testEval("/.", null, null, new XPathUnsupportedException());
//        //testEval("/..", null, null, new XPathUnsupportedException());
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
            double d = ((XPathNumericLiteral)predicates.elementAt(0)).d;
            if (d != (double)((int)d)) {
                throw new IllegalArgumentException("invalid position: " + d);
            }

            int multiplicity = (int)d - 1;
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
    /*
    private QuestionDef getSelectQuestion (boolean multi) {
        QuestionDef q = new QuestionDef(1, "blah",
                multi ? Constants.CONTROL_SELECT_MULTI : Constants.CONTROL_SELECT_ONE);

        q.addSelectItem("choice 1", "val1");
        q.addSelectItem("choice 2", "val2");
        q.addSelectItem("choice 3", "val3");
        q.addSelectItem("choice 4", "val4");

        return q;
    }
    */

    public FormInstance createTestInstance() {
        TreeElement data = new TreeElement("data");
        data.addChild(new TreeElement("path"));
        FormInstance instance = new FormInstance(data);
        return instance;
    }

    private EvaluationContext getFunctionHandlers() {
        EvaluationContext ec = new EvaluationContext(null);
        final Class[][] allPrototypes = {
                {Double.class, Double.class},
                {Double.class},
                {String.class, String.class},
                {Double.class, String.class, Boolean.class},
                {Boolean.class},
                {Boolean.class, Double.class, String.class, Date.class, CustomType.class}
        };

        ec.addFunctionHandler(new IFunctionHandler() {
            public String getName() {
                return "testfunc";
            }

            public Vector getPrototypes() {
                Vector p = new Vector();
                p.addElement(new Class[0]);
                return p;
            }

            public boolean rawArgs() {
                return false;
            }

            public boolean realTime() {
                return false;
            }

            public Object eval(Object[] args, EvaluationContext ec) {
                return Boolean.TRUE;
            }
        });

        ec.addFunctionHandler(new IFunctionHandler() {

            public String getName() {
                return "regex";
            }

            public Object eval(Object[] args, EvaluationContext ec) {
                System.out.println("EVAL REGEX TESTS:");
                for (int i = 0; i < args.length; i++) {
                    System.out.println("REGEX ARGS: " + args[i].toString());
                }


                return new Boolean(true); // String.re  args[0].

            }

            public Vector getPrototypes() {
                Vector p = new Vector();
                p.addElement(allPrototypes[2]);
                return p;
            }

            public boolean rawArgs() {
                return false;
            }

            public boolean realTime() {
                return false;
            }

        });


        ec.addFunctionHandler(new IFunctionHandler() {
            public String getName() {
                return "add";
            }

            public Vector getPrototypes() {
                Vector p = new Vector();
                p.addElement(allPrototypes[0]);
                return p;
            }

            public boolean rawArgs() {
                return false;
            }

            public boolean realTime() {
                return false;
            }

            public Object eval(Object[] args, EvaluationContext ec) {
                return new Double(((Double)args[0]).doubleValue() + ((Double)args[1]).doubleValue());
            }
        });

        ec.addFunctionHandler(new IFunctionHandler() {
            public String getName() {
                return "proto";
            }

            public Vector getPrototypes() {
                Vector p = new Vector();
                p.addElement(allPrototypes[0]);
                p.addElement(allPrototypes[1]);
                p.addElement(allPrototypes[2]);
                p.addElement(allPrototypes[3]);
                return p;
            }

            public boolean rawArgs() {
                return false;
            }

            public boolean realTime() {
                return false;
            }

            public Object eval(Object[] args, EvaluationContext ec) {
                return printArgs(args);
            }
        });

        ec.addFunctionHandler(new IFunctionHandler() {
            public String getName() {
                return "raw";
            }

            public Vector getPrototypes() {
                Vector p = new Vector();
                p.addElement(allPrototypes[3]);
                return p;
            }

            public boolean rawArgs() {
                return true;
            }

            public boolean realTime() {
                return false;
            }

            public Object eval(Object[] args, EvaluationContext ec) {
                return printArgs(args);
            }
        });

        ec.addFunctionHandler(new IFunctionHandler() {
            public String getName() {
                return "null-proto";
            }

            public Vector getPrototypes() {
                return null;
            }

            public boolean rawArgs() {
                return false;
            }

            public boolean realTime() {
                return false;
            }

            public Object eval(Object[] args, EvaluationContext ec) {
                return Boolean.FALSE;
            }
        });

        ec.addFunctionHandler(new IFunctionHandler() {
            public String getName() {
                return "concat";
            }

            public Vector getPrototypes() {
                return new Vector();
            }

            public boolean rawArgs() {
                return true;
            }

            public boolean realTime() {
                return false;
            }

            public Object eval(Object[] args, EvaluationContext ec) {
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < args.length; i++)
                    sb.append(XPathFuncExpr.toString(args[i]));
                return sb.toString();
            }
        });

        ec.addFunctionHandler(new IFunctionHandler() {
            public String getName() {
                return "convertible";
            }

            public Vector getPrototypes() {
                Vector p = new Vector();
                p.addElement(new Class[0]);
                return p;
            }

            public boolean rawArgs() {
                return false;
            }

            public boolean realTime() {
                return false;
            }

            public Object eval(Object[] args, EvaluationContext ec) {
                return new IExprDataType() {
                    public Boolean toBoolean() {
                        return Boolean.TRUE;
                    }

                    public Double toNumeric() {
                        return new Double(5.0);
                    }

                    public String toString() {
                        return "hi";
                    }
                };
            }
        });

        ec.addFunctionHandler(new IFunctionHandler() {
            public String getName() {
                return "inconvertible";
            }

            public Vector getPrototypes() {
                Vector p = new Vector();
                p.addElement(new Class[0]);
                return p;
            }

            public boolean rawArgs() {
                return false;
            }

            public boolean realTime() {
                return false;
            }

            public Object eval(Object[] args, EvaluationContext ec) {
                return new Object();
            }
        });

        ec.addFunctionHandler(new IFunctionHandler() {
            public String getName() {
                return "get-custom";
            }

            public Vector getPrototypes() {
                Vector p = new Vector();
                p.addElement(allPrototypes[4]);
                return p;
            }

            public boolean rawArgs() {
                return false;
            }

            public boolean realTime() {
                return false;
            }

            public Object eval(Object[] args, EvaluationContext ec) {
                return ((Boolean)args[0]).booleanValue() ? new CustomSubType() : new CustomType();
            }
        });

        ec.addFunctionHandler(new IFunctionHandler() {
            public String getName() {
                return "check-types";
            }

            public Vector getPrototypes() {
                Vector p = new Vector();
                p.addElement(allPrototypes[5]);
                return p;
            }

            public boolean rawArgs() {
                return false;
            }

            public boolean realTime() {
                return false;
            }

            public Object eval(Object[] args, EvaluationContext ec) {
                if (args.length != 5 || !(args[0] instanceof Boolean) || !(args[1] instanceof Double) ||
                        !(args[2] instanceof String) || !(args[3] instanceof Date) || !(args[4] instanceof CustomType))
                    fail("Types in custom function handler not converted properly/prototype not matched properly");

                return Boolean.TRUE;
            }
        });
        return ec;
    }

    private EvaluationContext getVariableContext() {
        EvaluationContext ec = new EvaluationContext(null);

        ec.setVariable("var_float_five", new Float(5.0));
        ec.setVariable("var_string_five", "five");
        ec.setVariable("var_int_five", new Integer(5));
        ec.setVariable("var_double_five", new Double(5.0));

        return ec;
    }

    private String printArgs(Object[] oa) {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = 0; i < oa.length; i++) {
            String fullName = oa[i].getClass().getName();
            int lastIndex = Math.max(fullName.lastIndexOf('.'), fullName.lastIndexOf('$'));
            sb.append(fullName.substring(lastIndex + 1, fullName.length()));
            sb.append(":");
            sb.append(oa[i] instanceof Date ? DateUtils.formatDate((Date)oa[i], DateUtils.FORMAT_ISO8601) : oa[i].toString());
            if (i < oa.length - 1)
                sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private class CustomType {
        public String val() {
            return "custom";
        }

        public String toString() {
            return "";
        }

        public boolean equals(Object o) {
            return o instanceof CustomType;
        }
    }

    private class CustomSubType extends CustomType {
        public String val() {
            return "custom-sub";
        }
    }

    /* unused
    private class CustomAnswerData implements IAnswerData {
        public String getDisplayText() { return "custom"; }
        public Object getValue() { return new CustomType(); }
        public void setValue(Object o) { }
        public void readExternal(DataInputStream in, PrototypeFactory pf) { }
        public void writeExternal(DataOutputStream out) { }
        public IAnswerData clone() {
            return new CustomAnswerData();
        }
    }
    */
    private abstract class StatefulFunc implements IFunctionHandler {
        public String val;

        public boolean rawArgs() {
            return false;
        }

        public boolean realTime() {
            return false;
        }
    }

    StatefulFunc read = new StatefulFunc() {
        public String getName() {
            return "read";
        }

        public Vector getPrototypes() {
            Vector p = new Vector();
            p.addElement(new Class[0]);
            return p;
        }

        public Object eval(Object[] args, EvaluationContext ec) {
            return val;
        }
    };

    StatefulFunc write = new StatefulFunc() {
        public String getName() {
            return "write";
        }

        public Vector getPrototypes() {
            Vector p = new Vector();
            Class[] proto = {String.class};
            p.addElement(proto);
            return p;
        }

        public Object eval(Object[] args, EvaluationContext ec) {
            val = (String)args[0];
            return Boolean.TRUE;
        }
    };
}


