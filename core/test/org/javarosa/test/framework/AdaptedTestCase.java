package org.javarosa.test.framework;

import junit.framework.TestCase;

/**
 * An adapter class which loads old j2me-unit style tests into standard
 * junit 3 tests.
 * 
 * Shouldn't be used in new tests. New tests should use normal Junit3
 * conventions
 * 
 * @author ctsims
 *
 */
public class AdaptedTestCase extends TestCase {
    TestMethod method;
    
    public AdaptedTestCase() {
        super();
    }
    
    public AdaptedTestCase(String name) {
        super(name);
    }
    
    public AdaptedTestCase(String name, TestMethod method) {
        super(name);
        this.method = method;
    }
    
    @Override
    protected void runTest() throws Throwable {
        method.run(this);
    }

    
}
