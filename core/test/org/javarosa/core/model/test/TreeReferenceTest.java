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

package org.javarosa.core.model.test;

import j2meunit.framework.Test;
import j2meunit.framework.TestCase;
import j2meunit.framework.TestMethod;
import j2meunit.framework.TestSuite;

import java.util.Vector;

import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.model.xform.XPathReference;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class TreeReferenceTest extends TestCase {

    private TreeReference root;
    private TreeReference aRef;
    private TreeReference bRef;
    private TreeReference acRef;
    private TreeReference acdRef;
    private TreeReference aceRef;
    private TreeReference bcRef;
    private TreeReference dotcRef;
    private TreeReference parentRef;

    private TreeReference dotRef;

    private TreeReference a2Ref;
    private TreeReference a2extRef;

    private TreeReference acPredRef;
    private TreeReference acPredMatchRef;
    private TreeReference acPredNotRef;


    public TreeReferenceTest(String name, TestMethod rTestMethod) {
        super(name, rTestMethod);
        initStuff();
    }

    public TreeReferenceTest(String name) {
        super(name);
        initStuff();
    }

    public TreeReferenceTest() {
        super();
        initStuff();
    }

    private void initStuff() {
        root = TreeReference.rootRef();
        aRef = root.extendRef("a", TreeReference.DEFAULT_MUTLIPLICITY);
        bRef = root.extendRef("b", TreeReference.DEFAULT_MUTLIPLICITY);
        acRef = aRef.extendRef("c", TreeReference.DEFAULT_MUTLIPLICITY);
        bcRef = bRef.extendRef("c", TreeReference.DEFAULT_MUTLIPLICITY);

        acdRef = acRef.extendRef("d", TreeReference.DEFAULT_MUTLIPLICITY);
        aceRef = acRef.extendRef("e", TreeReference.DEFAULT_MUTLIPLICITY);

        dotRef = TreeReference.selfRef();
        dotcRef = dotRef.extendRef("c", TreeReference.DEFAULT_MUTLIPLICITY);

        // represent ../
        parentRef = TreeReference.selfRef();
        parentRef.incrementRefLevel();

        a2Ref = root.extendRef("a", 2);
        a2extRef = root.extendRef("a", -1);
        a2extRef.setInstanceName("external");

        acPredRef = acRef.clone();
        acPredMatchRef = acRef.clone();
        acPredNotRef = acRef.clone();


    }

    public Test suite() {
        TestSuite aSuite = new TestSuite();
        System.out.println("Running TreeReference tests...");
        for (int i = 1; i <= NUM_TESTS; i++) {
            final int testID = i;
            aSuite.addTest(new TreeReferenceTest("TreeReference Test " + i, new TestMethod() {
                public void run(TestCase tc) {
                    ((TreeReferenceTest)tc).doTest(testID);
                }
            }));
        }

        return aSuite;
    }

    private final static int NUM_TESTS = 8;

    private void doTest(int i) {
        switch (i) {
            case 1:
                testClones();
                break;
            case 2:
                testSerialization();
                break;
            case 3:
                testParentage();
                break;
            case 4:
                testIntersection();
                break;
            case 5:
                contextualization();
                break;
            case 6:
                testPredicates();
                break;
            case 7:
                testGenericize();
                break;
            case 8:
                testSubreferences();
                break;
        }
    }


    private void testSubreferences() {
        if (!aRef.equals(acdRef.getSubReference(0))) {
            fail("(/a/c/d).subreference(0) should be: /a");
        }
        if (!acRef.equals(acdRef.getSubReference(1))) {
            fail("(/a/c/d).subreference(1) should be: /a/c");
        }
        try {
            parentRef.getSubReference(0);
            fail("(../).subreference(0) should throw an exception");
        } catch (IllegalArgumentException e) {
        }
    }

    private void testSerialization() {
        //TODO: That ^
    }


    private void testParentage() {
        if (!root.isParentOf(aRef, true)) {
            fail("/ is a parent of '/a'");
        }
        ;
        if (!aRef.isParentOf(acRef, true)) {
            fail("/a is a parent of '/a/c'");
        }
        ;
        if (!aRef.isParentOf(acdRef, true)) {
            fail("a is a parent of 'a/c/d'");
        }
        ;
        if (aRef.isParentOf(bcRef, true)) {
            fail("/a is not parent of '/b/c'");
        }
        ;

        if (aRef.isParentOf(dotcRef, true)) {
            fail("/a is not parent of './c'");
        }
        ;
    }

    private void testClones() {
        if (!aRef.clone().equals(aRef)) {
            fail("/a was unable to clone properly");
        }
        if (!acRef.clone().equals(acRef)) {
            fail("/a/c was unable to clone properly");
        }
        if (!dotRef.clone().equals(dotRef)) {
            fail(". was unable to clone properly");
        }
        if (!dotcRef.clone().equals(dotcRef)) {
            fail("./c was unable to clone properly");
        }
    }

    private void testIntersection() {
        if (!aRef.intersect(aRef).equals(aRef)) {
            fail("intersect(/a,/a) should result in /a");
        }
        if (!acRef.intersect(acRef).equals(acRef)) {
            fail("intersect(/a/c,/a/c) should result in /a/c");
        }
        if (!aRef.intersect(dotRef).equals(root)) {
            fail("intersect(/a,.) should result in /");
        }
        if (!acRef.intersect(aRef).equals(aRef)) {
            fail("intersect(/a/c,/a) should result in /a");
        }
        if (!aRef.intersect(acRef).equals(aRef)) {
            fail("intersect(/a,/a/c) should result in /a");
        }
        if (!aceRef.intersect(acdRef).equals(acRef)) {
            fail("intersect(/a/c/d,/a/c/e) should result in /a/c");
        }
        if (!aceRef.intersect(bRef).equals(root)) {
            fail("intersect(/a/c/e, /b) should result in /");
        }
        if (!dotRef.intersect(dotRef).equals(root)) {
            fail("intersect(.,.) should result in /");
        }
    }

    private void contextualization() {
        TreeReference abc = XPathReference.getPathExpr("/a/b/c").getReference();
        TreeReference ab = XPathReference.getPathExpr("/a/b").getReference();
        TreeReference ac = XPathReference.getPathExpr("/a/c").getReference();

        TreeReference floatc = XPathReference.getPathExpr("c").getReference();
        TreeReference floatc2 = XPathReference.getPathExpr("./c").getReference();
        TreeReference backc = XPathReference.getPathExpr("../c").getReference();

        TreeReference testabc = floatc.contextualize(ab);
        TreeReference testabc2 = floatc2.contextualize(ab);
        TreeReference testac = backc.contextualize(ab);

        TreeReference invalid = floatc.contextualize(floatc2);

        if (!abc.equals(testabc)) {
            fail("context: c didn't evaluate to " + abc.toString(true) + ", but rather to " + testabc.toString(true));
        }
        if (!abc.equals(testabc2)) {
            fail("context: ./c didn't evaluate to " + abc.toString(true) + ", but rather to " + testabc2.toString(true));
        }
        if (!ac.equals(testac)) {
            fail("context: ../c didn't evaluate to " + ac.toString(true) + ", but rather to " + testac.toString(true));
        }
        if (invalid != null) {
            fail("was succesfully able to contextualize against an ambiguous reference. Result was: " + invalid.toString(true));
        }

        TreeReference a2extc = a2extRef.contextualize(a2Ref);
        if (a2extc.getMultLast() == 2) {
            fail("Treeref from named instance wrongly accepted multiplicity context from root instance");
        }
    }

    private void testPredicates() {
        XPathExpression testPred = null;
        XPathExpression failPred = null;
        try {
            testPred = XPathParseTool.parseXPath("../b = 'test'");
            failPred = XPathParseTool.parseXPath("../b = 'fail'");
        } catch (XPathSyntaxException e) {
            fail("Bad tests! Rewrite xpath expressions for predicate tests");
        }

        Vector<XPathExpression> apreds = new Vector<XPathExpression>();
        Vector<XPathExpression> amatchpreds = new Vector<XPathExpression>();
        Vector<XPathExpression> anotpreds = new Vector<XPathExpression>();

        apreds.add(testPred);
        amatchpreds.add(testPred);
        anotpreds.add(failPred);

        acPredRef.addPredicate(0, apreds);
        acPredMatchRef.addPredicate(0, amatchpreds);
        acPredNotRef.addPredicate(0, anotpreds);

        assertTrue("Predicates weren't correctly removed from reference.",
                !acPredRef.removePredicates().hasPredicates());

        assertTrue("Predicates weren't correctly detected.",
                acPredRef.hasPredicates());

        assertTrue("Found predicates where they shouldn't be.",
                acPredRef.getPredicate(1) == null);

        assertTrue("Didn't find predicates where they should be.",
                acPredRef.getPredicate(0) == apreds);

        assertTrue("/a[..b = 'test'] Did not equal itself!",
                acPredRef.equals(acPredMatchRef));

        assertTrue("/a[..b = 'test'] was equal to /a[..b = 'fail']",
                !acPredRef.equals(acPredNotRef));
    }


    private void testGenericize() {
        // Generic ref to generic attribute
        TreeReference attributeRef =
            XPathReference.getPathExpr("/data/node/@attribute").getReference();

        // re-genericize
        TreeReference genericRef = attributeRef.genericize();

        if (!attributeRef.equals(genericRef)) {
            fail("Genericize improperly converted " + attributeRef.toString(true) +
                    " to " + genericRef.toString(true));
        }

        // (/data/aRef[3]).genericize() ==> /data/aRef (with aRef's multiplicity being -1)
        if (!aRef.genericize().equals(a2Ref.genericize())) {
            fail("Genericize improperly converted removed multiplicities of " + 
                    a2Ref.toString(true) +
                    ", which should, once genericized, should match" +
                    aRef.genericize().toString(true));
        }
        // (/data/aRef[3]).genericize() ==> /data/aRef (with aRef's multiplicity being -1)
        // but 'aRef' in aRef should have the default multiplicity of 0
        if (aRef.equals(a2Ref.genericize())) {
            fail("Genericize improperly converted removed multiplicities of " + 
                    a2Ref.toString(true) +
                    ", which should, once genericized, should match" +
                    aRef.toString(true));
        }
    }
}
