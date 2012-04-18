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

import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.model.xform.XPathReference;

public class TreeReferenceTest extends TestCase {
	
	TreeReference root;
	TreeReference a;
	TreeReference b;
	TreeReference ac;
	TreeReference acd;
	TreeReference ace;
	TreeReference bc;
	TreeReference dotc;
	
	TreeReference dot;
	
	
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
	
	public void initStuff(){
		root = TreeReference.rootRef();
		a = root.extendRef("a", TreeReference.DEFAULT_MUTLIPLICITY);
		b = root.extendRef("b", TreeReference.DEFAULT_MUTLIPLICITY);
		ac = a.extendRef("c", TreeReference.DEFAULT_MUTLIPLICITY);
		bc = b.extendRef("c", TreeReference.DEFAULT_MUTLIPLICITY);
		
		acd = ac.extendRef("d", TreeReference.DEFAULT_MUTLIPLICITY);
		ace = ac.extendRef("e", TreeReference.DEFAULT_MUTLIPLICITY);
		
		dot = TreeReference.selfRef();
		dotc = dot.extendRef("c", TreeReference.DEFAULT_MUTLIPLICITY);
	}
	
	public Test suite() {
		TestSuite aSuite = new TestSuite();
		System.out.println("Running TreeReference tests...");
		for (int i = 1; i <= NUM_TESTS; i++) {
			final int testID = i;
			aSuite.addTest(new TreeReferenceTest("TreeReference Test " + i, new TestMethod() {
				public void run (TestCase tc) {
					((TreeReferenceTest)tc).doTest(testID);
				}
			}));
		}
			
		return aSuite;
	}
	
	public final static int NUM_TESTS = 5;
	public void doTest (int i) {
		switch (i) {
		case 1: testClones(); break;
		case 2: testSerialization(); break;
		case 3: testParentage(); break;
		case 4: testIntersection(); break;
		case 5: contextualization(); break;
		}
	}
	
	public void testSerialization () {
		//TODO: That ^
	}

	
	public void testParentage () {
		if(!root.isParentOf(a, true)) { fail("/ is a parent of '/a'"); };
		if(!a.isParentOf(ac, true)) { fail("/a is a parent of '/a/c'"); };
		if(a.isParentOf(bc, true)) { fail("/a is not parent of '/b/c'"); };
		
		if(a.isParentOf(dotc, true)) { fail("/a is not parent of './c'"); };
	}
	
	public void testClones() {
		if(!a.clone().equals(a)) { fail("/a was unable to clone properly"); }
		if(!ac.clone().equals(ac)) { fail("/a/c was unable to clone properly"); }
		if(!dot.clone().equals(dot)) { fail(". was unable to clone properly"); }
		if(!dotc.clone().equals(dotc)) { fail("./c was unable to clone properly"); }
	}
	
	public void testIntersection() {
		if(!a.intersect(a).equals(a)) { fail("intersect(/a,/a) should result in /a");}
		if(!ac.intersect(ac).equals(ac)) { fail("intersect(/a/c,/a/c) should result in /a/c");}
		if(!a.intersect(dot).equals(root)) { fail("intersect(/a,.) should result in /");}
		if(!ac.intersect(a).equals(a)) { fail("intersect(/a/c,/a) should result in /a");}
		if(!a.intersect(ac).equals(a)) { fail("intersect(/a,/a/c) should result in /a");}
		if(!ace.intersect(acd).equals(ac)) { fail("intersect(/a/c/d,/a/c/e) should result in /a/c");}
		if(!ace.intersect(b).equals(root)) { fail("intersect(/a/c/e, /b) should result in /");}
		if(!dot.intersect(dot).equals(root)) { fail("intersect(.,.) should result in /");}
	}
	
	public void contextualization() {
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
		
		if(!abc.equals(testabc)) { fail("context: c didn't evaluate to " + abc.toString(true) + ", but rather to " + testabc.toString(true)); }
		if(!abc.equals(testabc2)) { fail("context: ./c didn't evaluate to " + abc.toString(true) + ", but rather to " + testabc2.toString(true)); }
		if(!ac.equals(testac)) { fail("context: ../c didn't evaluate to " + ac.toString(true) + ", but rather to " + testac.toString(true)); }
		if(invalid != null) { fail("was succesfully able to contextualize against an ambiguous reference. Result was: " + invalid.toString(true));}
	}
}

