package org.javarosa.core.model.data.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Date;

import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.utils.DateUtils;

public class DateDataTests extends TestCase {

    Date today;
    Date notToday;

    private static int NUM_TESTS = 4;

    /* (non-Javadoc)
     * @see j2meunit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        today = DateUtils.roundDate(new Date());
        notToday = DateUtils.roundDate(new Date(today.getTime() - today.getTime() / 2));
    }

    public DateDataTests(String name) {
        super(name);
    }

    public DateDataTests() {
        super();
    }

    public Test suite() {
        TestSuite suite = new TestSuite();

        suite.addTest(new DateDataTests("testGetData");
        suite.addTest(new DateDataTests("testSetData");
        suite.addTest(new DateDataTests("testDisplay");
        suite.addTest(new DateDataTests("testNullData");

        return suite;
    }


    public void testGetData() {
        DateData data = new DateData(today);
        assertEquals("DateData's getValue returned an incorrect date", data.getValue(), today);
        Date temp = new Date(today.getTime());
        today.setTime(1234);
        assertEquals("DateData's getValue was mutated incorrectly", data.getValue(), temp);

        Date rep = (Date)data.getValue();
        rep.setTime(rep.getTime() - 1000);

        assertEquals("DateData's getValue was mutated incorrectly", data.getValue(), temp);
    }

    public void testSetData() {
        DateData data = new DateData(notToday);
        data.setValue(today);

        assertTrue("DateData did not set value properly. Maintained old value.", !(data.getValue().equals(notToday)));
        assertEquals("DateData did not properly set value ", data.getValue(), today);

        data.setValue(notToday);
        assertTrue("DateData did not set value properly. Maintained old value.", !(data.getValue().equals(today)));
        assertEquals("DateData did not properly reset value ", data.getValue(), notToday);

        Date temp = new Date(notToday.getTime());
        notToday.setTime(notToday.getTime() - 1324);

        assertEquals("DateData's value was mutated incorrectly", data.getValue(), temp);
    }

    public void testDisplay() {
        // We don't actually want this, because the Date's getDisplayText code should be moved to a library
    }

    public void testNullData() {
        boolean exceptionThrown = false;
        DateData data = new DateData();
        data.setValue(today);
        try {
            data.setValue(null);
        } catch (NullPointerException e) {
            exceptionThrown = true;
        }
        assertTrue("DateData failed to throw an exception when setting null data", exceptionThrown);
        assertTrue("DateData overwrote existing value on incorrect input", data.getValue().equals(today));
    }
}
