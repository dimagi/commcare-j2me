package org.javarosa.test.framework;

/**
 * An adapter interface which transitions j2me unit 
 * test runners into normal junit 3 tests.
 * 
 * Shouldn't be used in new tests.
 * 
 * 
 * @author ctsims
 *
 */
public interface TestMethod {
    public void run(AdaptedTestCase tc);
}
