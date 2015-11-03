package org.javarosa.xpath.expr.test;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.core.model.instance.utils.FormLoadingUtils;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.fail;

/**
 * @author Phillip Mates
 */
public class XPathPathExprTest {
    @Test
    public void testHeterogeneousPaths() {
        FormInstance instance = loadInstance("/test_xpathpathexpr.xml");

        // Used to reproduce bug where locations can't handle heterogeneous template paths.
        // This bug has been fixed and the following test now passes.
        testEval("/data/places/country[@id ='two']/state[@id = 'beehive_state']", instance, null, "Utah");
        testEval("/data/places/country[@id ='one']/name", instance, null, "Singapore");
    }

    @Test
    public void testOutOfBoundsSelect() {
        FormInstance instance = loadInstance("/test_xpathpathexpr.xml");

        // normal selected-at behaviour
        testEval("selected-at('hello there', 0)", instance, null, "hello");
        testEval("selected-at('hello there', 1)", instance, null, "there");
        // out of bounds selection should raise an XPathException
        testEval("selected-at('hello there', 2)", instance, null, new XPathException());
    }

    /**
     * Some simple xpath expressions with multiple predicates that operate over
     * nodesets.
     */
    @Test
    public void testNestedMultiplicities() {
        FormParseInit fpi = new FormParseInit("/test_nested_multiplicities.xml");
        FormDef fd = fpi.getFormDef();

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

    /**
     * Test nested predicates that have relative and absolute references.
     */
    @Test
    public void testNestedPreds() {
        FormParseInit fpi = new FormParseInit("/test_nested_preds_with_rel_refs.xml");
        FormDef fd = fpi.getFormDef();
        FormInstance fi = fd.getInstance();
        FormInstance groupsInstance = (FormInstance)fd.getNonMainInstance("groups");
        EvaluationContext ec = fd.getEvaluationContext();

        // TODO PLM: test chaining of predicates where second pred would throw
        // and error if the first pred hadn't already filtered out certain
        // nodes:
        // /a/b[filter out first][../a/b/d = foo]

        testEval("join(' ', instance('groups')/root/groups/group/@id)",
                groupsInstance, ec, "inc dwa");

        testEval("count(instance('groups')/root/groups[position() = 1]/team[@id = 'mobile'])",
                groupsInstance, ec, 1.0);

        // find 'group' elements that have a 'team' sibling with id = mobile;
        testEval("instance('groups')/root/groups/group[count(../team[@id = 'mobile']) > 0]/@id",
                groupsInstance, ec, "inc");

        testEval("count(instance('groups')/root/groups/group[count(../team[@id = 'mobile']) > 0]) = 1",
                groupsInstance, ec, true);

        testEval("if(count(instance('groups')/root/groups/group/group_data/data) > 0 and count(instance('groups')/root/groups/group[count(../team[@id = 'mobile']) > 0]) = 1, instance('groups')/root/groups/group[count(../team[@id = 'mobile']) > 0]/@id, '')",
                groupsInstance, ec, "inc");

        testEval("instance('groups')/root/groups/group[count(group_data/data[@key = 'all_field_staff' and . = 'yes']) > 0]/@id",
                groupsInstance, ec, "inc");

        testEval("if(count(instance('groups')/root/groups/group/group_data/data) > 0 and count(instance('groups')/root/groups/group[count(group_data/data[@key = 'all_field_staff' and . ='yes']) > 0]) = 1, instance('groups')/root/groups/group[count(group_data/data[@key = 'all_field_staff' and . ='yes']) > 0]/@id, '')",
                groupsInstance, ec, "inc");
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
            fail("Unable to load form at " + formPath);
        } catch (InvalidStructureException e) {
            fail("Form at " + formPath + " has an invalid structure.");
        }
        return instance;
    }
}
