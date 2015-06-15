package org.javarosa.core.io.test;

import org.junit.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

import org.javarosa.core.io.BufferedInputStream;
import org.javarosa.core.util.ArrayUtilities;

public class BufferedInputStreamTests extends TestCase {

    byte[] bytes;

    int[] sizesToTest = new int[]{15, 64, 500, 1280, 2047, 2048, 2049, 5000, 10000, 23000};

    byte[][] arraysToTest;

    private static int NUM_TESTS = 2;

    /* (non-Javadoc)
     * @see j2meunit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        Random r = new Random();

        arraysToTest = new byte[sizesToTest.length][];
        for (int i = 0; i < sizesToTest.length; ++i) {
            arraysToTest[i] = new byte[sizesToTest[i]];
            r.nextBytes(arraysToTest[i]);
        }
    }

    public BufferedInputStreamTests(String name) {
        super(name);
    }

    public BufferedInputStreamTests() {
        super();
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        suite.addTest(new BufferedInputStreamTests("testBuffered"));
        suite.addTest(new BufferedInputStreamTests("testIndividual"));

        return suite;
    }

    @Test
    public void testBuffered() {
        //TODO: Test on this axis too?
        byte[] testBuffer = new byte[256];

        for (byte[] bytes : arraysToTest) {
            try {
                BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(bytes));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                boolean done = false;
                while (!done) {
                    int read = bis.read(testBuffer);
                    if (read == -1) {
                        break;
                    } else {
                        baos.write(testBuffer, 0, read);
                    }
                }


                if (!ArrayUtilities.arraysEqual(bytes, baos.toByteArray())) {
                    this.fail("Bulk BufferedInputStream read failed at size " + bytes.length);
                }
            } catch (Exception e) {
                this.fail("Exception while testing bulk read for " + bytes.length + " size: " + e.getMessage());
                continue;
            }
        }
    }

    @Test
    public void testIndividual() {
        //TODO: Almost identical to above
        for (byte[] bytes : arraysToTest) {
            try {
                BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(bytes));
                int position = 0;

                boolean done = false;
                while (!done) {
                    int read = bis.read();
                    if (read == -1) {
                        break;
                    } else {
                        if (bytes[position] != (byte)read) {
                            this.fail("one-by-one BIS read failed at size " + bytes.length + " at position " + position);
                        }
                    }
                    position++;
                }
                if (position != bytes.length) {
                    this.fail("one-by-one BIS read failed to read full array of size " + bytes.length + " only read " + position);
                }
            } catch (Exception e) {
                this.fail("Exception while testing buffered read for " + bytes.length + " size: " + e.getMessage());
                continue;
            }
        }
    }
}
