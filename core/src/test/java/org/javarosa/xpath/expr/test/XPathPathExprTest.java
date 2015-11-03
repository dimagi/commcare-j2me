package org.javarosa.xpath.expr.test;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.test.FormParseInit;
import org.javarosa.core.model.instance.utils.FormLoadingUtils;
import org.javarosa.test_utils.ExprEvalUtils;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xpath.XPathTypeMismatchException;
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
        ExprEvalUtils.testEval("/data/places/country[@id ='two']/state[@id = 'beehive_state']", instance, null, "Utah");
        ExprEvalUtils.testEval("/data/places/country[@id ='one']/name", instance, null, "Singapore");
    }

    /**
     * Some simple xpath expressions with multiple predicates that operate over
     * nodesets.
     */
    @Test
    public void testNestedMultiplicities() {
        FormParseInit fpi = new FormParseInit("/test_nested_multiplicities.xml");
        FormDef fd = fpi.getFormDef();

        ExprEvalUtils.testEval("/data/bikes/manufacturer/model[@id='pista']/@color",
                fd.getInstance(), null, "seafoam");
        ExprEvalUtils.testEval("join(' ', /data/bikes/manufacturer[@american='yes']/model[.=1]/@id)",
                fd.getInstance(), null, "karate-monkey vamoots");
        ExprEvalUtils.testEval("count(/data/bikes/manufacturer[@american='yes'][count(model[.=1]) > 0]/model/@id)",
                fd.getInstance(), null, 4.0);
        ExprEvalUtils.testEval("join(' ', /data/bikes/manufacturer[@american='yes'][count(model[.=1]) > 0]/model/@id)",
                fd.getInstance(), null, "karate-monkey long-haul cross-check vamoots");
        ExprEvalUtils.testEval("join(' ', /data/bikes/manufacturer[@american='yes'][count(model=1) > 0]/model/@id)",
                fd.getInstance(), null, new XPathTypeMismatchException());
        ExprEvalUtils.testEval("join(' ', /data/bikes/manufacturer[@american='no'][model=1]/model/@id)",
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

        ExprEvalUtils.testEval("join(' ', instance('groups')/root/groups/group/@id)",
                groupsInstance, ec, "inc dwa");

        ExprEvalUtils.testEval("count(instance('groups')/root/groups[position() = 1]/team[@id = 'mobile'])",
                groupsInstance, ec, 1.0);

        // find 'group' elements that have a 'team' sibling with id = mobile;
        ExprEvalUtils.testEval("instance('groups')/root/groups/group[count(../team[@id = 'mobile']) > 0]/@id",
                groupsInstance, ec, "inc");

        ExprEvalUtils.testEval("count(instance('groups')/root/groups/group[count(../team[@id = 'mobile']) > 0]) = 1",
                groupsInstance, ec, true);

        ExprEvalUtils.testEval("if(count(instance('groups')/root/groups/group/group_data/data) > 0 and count(instance('groups')/root/groups/group[count(../team[@id = 'mobile']) > 0]) = 1, instance('groups')/root/groups/group[count(../team[@id = 'mobile']) > 0]/@id, '')",
                groupsInstance, ec, "inc");

        ExprEvalUtils.testEval("instance('groups')/root/groups/group[count(group_data/data[@key = 'all_field_staff' and . = 'yes']) > 0]/@id",
                groupsInstance, ec, "inc");

        ExprEvalUtils.testEval("if(count(instance('groups')/root/groups/group/group_data/data) > 0 and count(instance('groups')/root/groups/group[count(group_data/data[@key = 'all_field_staff' and . ='yes']) > 0]) = 1, instance('groups')/root/groups/group[count(group_data/data[@key = 'all_field_staff' and . ='yes']) > 0]/@id, '')",
                groupsInstance, ec, "inc");
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
