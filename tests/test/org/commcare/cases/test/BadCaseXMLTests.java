package org.commcare.cases.test;

import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.junit.Before;
import org.junit.Test;

/**
 * Test suite to verify end-to-end parsing of inbound case XML
 * and reading values back from the casedb model
 *
 * @author ctsims
 */
public class BadCaseXMLTests {

    private MockUserDataSandbox sandbox;

    @Before
    public void setUp() {
        sandbox = MockDataUtils.getStaticStorage();
    }

    @Test
    public void testNoCaseID() {
        //Expected - Fail silently (TODO: Fix?)
        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/case_create_broken_no_caseid.xml"), sandbox);

        //Make sure that we didn't make a case entry for the bad case though
        //assertEquals("Case XML with no id should not have created a case record", sandbox.getCaseStorage().getNumRecords(), 0);
    }
}
