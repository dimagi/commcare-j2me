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
    private TreeReference ac2Ref;
    private TreeReference acdRef;
    private TreeReference aceRef;
    private TreeReference bcRef;
    private TreeReference dotcRef;
    private TreeReference parentRef;

    private TreeReference a;
    private TreeReference aa;
    private TreeReference aaa;

    private TreeReference dotRef;

    private TreeReference floatc;
    private TreeReference floatc2;
    private TreeReference backc;
    private TreeReference back2c;

    private TreeReference a2Ref;
    private TreeReference a2extRef;

    private TreeReference abcRef;
    private TreeReference abRef;

    private TreeReference acPredRef;
    private TreeReference acPredMatchRef;
    private TreeReference acPredNotRef;
    private TreeReference acPredRefClone;
    private Vector<XPathExpression> apreds;
    private Vector<XPathExpression> amatchpreds;
    private Vector<XPathExpression> anotpreds;


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

        abcRef = XPathReference.getPathExpr("/a/b/c").getReference();
        ac2Ref = XPathReference.getPathExpr("/a/c").getReference();
        abRef = XPathReference.getPathExpr("/a/b").getReference();

        dotRef = TreeReference.selfRef();
        dotcRef = dotRef.extendRef("c", TreeReference.DEFAULT_MUTLIPLICITY);

        // setup /a[2]/a[3]/a[4] reference
        a = root.extendRef("a", 1);
        aa = a.extendRef("a", 2);
        aaa = aa.extendRef("a", 3);

        // some relative references
        floatc = XPathReference.getPathExpr("c").getReference();
        floatc2 = XPathReference.getPathExpr("./c").getReference();
        backc = XPathReference.getPathExpr("../c").getReference();
        back2c = XPathReference.getPathExpr("../../c").getReference();

        // represent ../
        parentRef = TreeReference.selfRef();
        parentRef.incrementRefLevel();

        a2Ref = root.extendRef("a", 2);
        a2extRef = root.extendRef("a", TreeReference.INDEX_UNBOUND);
        a2extRef.setInstanceName("external");

        acPredRef = acRef.clone();
        acPredMatchRef = acRef.clone();
        acPredNotRef = acRef.clone();

        XPathExpression testPred = null;
        XPathExpression failPred = null;
        XPathExpression passPred = null;
        try {
            testPred = XPathParseTool.parseXPath("../b = 'test'");
            failPred = XPathParseTool.parseXPath("../b = 'fail'");
            passPred = XPathParseTool.parseXPath("true() = true()");
        } catch (XPathSyntaxException e) {
            fail("Bad tests! Rewrite xpath expressions for predicate tests");
        }

        apreds = new Vector<XPathExpression>();
        amatchpreds = new Vector<XPathExpression>();
        anotpreds = new Vector<XPathExpression>();

        apreds.add(testPred);
        amatchpreds.add(testPred);
        anotpreds.add(failPred);

        acPredRef.addPredicate(0, apreds);
        acPredMatchRef.addPredicate(0, amatchpreds);
        acPredNotRef.addPredicate(0, anotpreds);


        //For mutation testing.

        acPredRefClone = acPredRef.clone();

        //We know we have a predicate at the 0 position
        Vector<XPathExpression> acPredRefClonePredicates = acPredRefClone.getPredicate(0);

        //Update it to add a new predicate
        acPredRefClonePredicates.add(passPred);

        //Reset the predicates in our new object
        acPredRefClone.addPredicate(0, acPredRefClonePredicates);
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

    private final static int NUM_TESTS = 10;

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
                testAnchor();
                break;
            case 6:
                testParent();
                break;
            case 7:
                testContextualization();
                break;
            case 8:
                testPredicates();
                break;
            case 9:
                testGenericize();
                break;
            case 10:
                testSubreferences();
                break;
            case 11:
                testMutation();
                break;
        }
    }

    //Tests ensuring that original references aren't mutated.
    private void testMutation() {
        assertTrue("/a/c[] predicate set illegally modified", acPredRef.getPredicate(0).size() != 1);
    }

    private void testSubreferences() {
        assertTrue("(/a/c/d).subreference(0) should be: /a",
                aRef.equals(acdRef.getSubReference(0)));
        assertTrue("(/a/c/d).subreference(1) should be: /a/c",
                acRef.equals(acdRef.getSubReference(1)));
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
        assertTrue("/ is a parent of '/a'",
                root.isParentOf(aRef, true));
        assertTrue("/a is a parent of '/a/c'",
                aRef.isParentOf(acRef, true));
        assertTrue("/a is an improper parent of '/a'",
                aRef.isParentOf(aRef, false));
        assertTrue("a is a parent of 'a/c/d'",
                aRef.isParentOf(acdRef, true));
        assertTrue("/a is not parent of '/b/c'",
                !aRef.isParentOf(bcRef, true));
        assertTrue("/a is not parent of './c'",
                !aRef.isParentOf(dotcRef, true));
        assertTrue("/a[2]/a[3] is a parent of '/a[2]/a[3]/a[4]'",
                aa.isParentOf(aaa, true));
    }

    private void testClones() {
        assertTrue("/a was unable to clone properly",
                aRef.clone().equals(aRef));
        assertTrue("/a/c was unable to clone properly",
                acRef.clone().equals(acRef));
        assertTrue(". was unable to clone properly",
                dotRef.clone().equals(dotRef));
        assertTrue("./c was unable to clone properly",
                dotcRef.clone().equals(dotcRef));
        assertTrue("/a[2]/a[3]/a[4] was unable to clone properly",
                aaa.clone().equals(aaa));
        assertTrue("/a[..b = 'test'] was unable to clone properly",
                acPredRef.clone().equals(acPredRef));

    }

    private void testIntersection() {
        assertTrue("intersect(/a, /a) should result in /a",
                aRef.intersect(aRef).equals(aRef));
        assertTrue("intersect(/a/c, /a/c) should result in /a/c",
                acRef.intersect(acRef).equals(acRef));
        assertTrue("intersect(/a, .) should result in /",
                aRef.intersect(dotRef).equals(root));
        assertTrue("intersect(/a/c, /a) should result in /a",
                acRef.intersect(aRef).equals(aRef));
        assertTrue("intersect(/a, /a/c) should result in /a",
                aRef.intersect(acRef).equals(aRef));
        assertTrue("intersect(/a/c/d, /a/c/e) should result in /a/c",
                aceRef.intersect(acdRef).equals(acRef));
        assertTrue("intersect(/a/c/e, /b) should result in /",
                aceRef.intersect(bRef).equals(root));
        assertTrue("intersect(.,.) should result in /",
                dotRef.intersect(dotRef).equals(root));
    }

    private void testContextualization() {
        TreeReference contextualizeEval;

        // ('c').contextualize('/a/b') ==> /a/b/c
        contextualizeEval = floatc.contextualize(abRef);
        assertTrue("context: c didn't evaluate to " + abcRef.toString() +
                        ", but rather to " + contextualizeEval.toString(),
                abcRef.equals(contextualizeEval));

        // ('./c').contextualize('/a/b') ==> /a/b/c
        contextualizeEval = floatc2.contextualize(abRef);
        assertTrue("context: ./c didn't evaluate to " + abcRef.toString() +
                        ", but rather to " + contextualizeEval.toString(),
                abcRef.equals(contextualizeEval));

        // TODO: investigate why this test fails when acRef is used instead of
        // ac2Ref
        // ('../c').contextualize('/a/b') ==> /a/c
        contextualizeEval = backc.contextualize(abRef);
        assertTrue("context: ../c didn't evaluate to " + ac2Ref.toString() +
                        ", but rather to " + contextualizeEval.toString(),
                ac2Ref.equals(contextualizeEval));

        // ('c').contextualize('./c') ==> null
        contextualizeEval = floatc.contextualize(floatc2);
        assertTrue("Was successfully able to contextualize against an ambiguous reference.",
                contextualizeEval == null);

        // ('a[-1]').contextualize('a[2]') ==> something like a[position() != 2]
        contextualizeEval = a2extRef.contextualize(a2Ref);
        assertTrue("Treeref from named instance wrongly accepted multiplicity " +
                        "context from root instance",
                contextualizeEval.getMultLast() != 2);

        // Two tests trying to figure out multiplicity copying during
        // contextualization.

        // Test trying to figure out multiplicity copying during contextualization
        // setup ../../a[5] reference
        TreeReference a5 = root.extendRef("a", 5);
        a5.setRefLevel(2);

        // setup expected result /a[2]/a[6] reference
        TreeReference expectedAs = a.extendRef("a", 5);

        // ('../../a[6]').contextualize('/a[2]/a[3]/a[4]') ==> /a[2]/a[6]
        contextualizeEval = a5.contextualize(aaa);
        assertTrue("Got " + contextualizeEval.toString() + " and expected /a[2]/a[6]" +
                        " for test of multiplicity copying when level names are same between refs",
                expectedAs.equals(contextualizeEval));

        // ('c').contextualize('/a/*') ==> /a/*/c
        TreeReference wildA = XPathReference.getPathExpr("/a/*").getReference();
        contextualizeEval = floatc.contextualize(wildA);
        assertTrue("Got " + contextualizeEval.toString() + " and expected /a/*/c" +
                        " for test of wildcard merging",
                wildA.extendRef("c", TreeReference.INDEX_UNBOUND).equals(contextualizeEval));

        // ('../*').contextualize('/a/c') ==> /a/*/
        // NOTE: It's unclear what should the behavoir for this be: /a/c or /a/*
        //       For now we will leave the code untouched and expect a/c
        TreeReference wildBack = XPathReference.getPathExpr("../*").getReference();
        contextualizeEval = wildBack.contextualize(acRef);
        assertTrue("Got " + contextualizeEval.toString() + " and expected /a/c" +
                        " for test of wildcard merging",
                acRef.genericize().equals(contextualizeEval.genericize()));

        // ('../a[6]').contextualize('/a/*/a') ==> /a/*/a[6]
        TreeReference wildAs = XPathReference.getPathExpr("/a/*").getReference();
        wildAs = wildAs.extendRef("a", TreeReference.DEFAULT_MUTLIPLICITY);
        a5.setRefLevel(1);
        contextualizeEval = a5.contextualize(wildAs);
        expectedAs = XPathReference.getPathExpr("/a/*").getReference().extendRef("a", 5);
        assertTrue("Got " + contextualizeEval.toString() + " and expected /a/*/a[6]" +
                        " for test of multiplicity copying when level names are same between refs",
                expectedAs.equals(contextualizeEval));

        // ('../../a[6]').contextualize('/a/*/a') ==> /a/a[6]
        wildAs = XPathReference.getPathExpr("/a/*/a").getReference();
        a5.setRefLevel(2);
        contextualizeEval = a5.contextualize(wildAs);
        expectedAs = XPathReference.getPathExpr("/a").getReference().extendRef("a", 5);
        assertTrue("Got " + contextualizeEval.toString() + " and expected /a/a[6]" +
                        " during specializing wildcard during contextualization.",
                expectedAs.equals(contextualizeEval));

    }

    private void testAnchor() {
        TreeReference anchorEval;
        // TODO: investigate why this test fails when acRef is used instead of
        // ac2Ref
        // ('../c').anchor('/a/b') ==> a/c
        anchorEval = backc.anchor(abRef);
        assertTrue("/a/b/ + ../c should anchor to /a/c not " + anchorEval.toString(),
                ac2Ref.equals(anchorEval));

        // return clone if absolute ref is being anchored to something
        // ('/a/c').anchor('./c') ==> a/c
        anchorEval = ac2Ref.anchor(floatc);
        assertTrue("./c + /a/c should just return /a/c not " + anchorEval.toString(),
                ac2Ref.equals(anchorEval));

        // ('./c').anchor('./c') ==> null
        anchorEval = floatc.anchor(floatc);
        assertTrue("./c + ./c should return null since trying to anchor to a relative ref",
                anchorEval == null);

        // ('../c').anchor('/a') ==> null
        anchorEval = back2c.anchor(aRef);
        assertTrue("/a + ../../c should return null since there are too many ../'s",
                anchorEval == null);

        // ('../*').anchor('/a/z') ==> /a/*/
        TreeReference wildBack = XPathReference.getPathExpr("../*").getReference();
        TreeReference wildA = XPathReference.getPathExpr("/a/*").getReference();
        TreeReference azRef = XPathReference.getPathExpr("/a/z").getReference();
        anchorEval = wildBack.anchor(azRef);
        assertTrue("Got " + anchorEval.toString() + " and expected " + wildA.toString() +
                        " for anchoring with wildcards",
                wildA.equals(anchorEval));
    }

    private void testParent() {
        TreeReference parentEval;

        // ('/a/b').parent('/a/c') ==> '/a/b'
        parentEval = abRef.parent(acRef);
        assertTrue("taking the parent of an absolute ref should return a copy of that ref",
                abRef.equals(parentEval));

        // ('../c').parent('/a/b') ==> null
        parentEval = backc.parent(abRef);
        assertTrue("you can't take the parent of a relative reference, unless it is also relative",
                parentEval == null);

        // ('../c').parent('../c') ==> null
        parentEval = backc.parent(backc);
        assertTrue("The argument to calling 'parent' on a relative reference " +
                        "must be a series of ../'s with no reference level data",
                parentEval == null);

        // ('../c').parent('../') ==> '../../c/'
        parentEval = backc.parent(parentRef);
        assertTrue("calling 'parent' on a relative reference with ../'s should join " +
                        "them with those of the level-less relative argument reference.",
                back2c.equals(parentEval));

        // ('./c').parent('/a/b') ==> '/a/b/c'
        parentEval = floatc.parent(abRef);
        assertTrue("standard call to 'parent' returned " + parentEval.toString() +
                        "instead of expected /a/b/c",
                abcRef.equals(parentEval));
    }

    private void testPredicates() {
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
            fail("Genericize improperly converted " + attributeRef.toString() +
                    " to " + genericRef.toString());
        }

        // (/data/aRef[3]).genericize() ==> /data/aRef (with aRef's multiplicity being -1)
        if (!aRef.genericize().equals(a2Ref.genericize())) {
            fail("Genericize improperly converted removed multiplicities of " +
                    a2Ref.toString() +
                    ", which should, once genericized, should match" +
                    aRef.genericize().toString());
        }
        // (/data/aRef[3]).genericize() ==> /data/aRef (with aRef's multiplicity being -1)
        // but 'aRef' in aRef should have the default multiplicity of 0
        if (aRef.equals(a2Ref.genericize())) {
            fail("Genericize improperly converted removed multiplicities of " +
                    a2Ref.toString() +
                    ", which should, once genericized, should match" +
                    aRef.toString());
        }
    }
}
