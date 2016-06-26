package org.commcare.test.utilities;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * JUnit runner for running case purge tests based on an external file resource.
 *
 * With this class set as the JUnit runner, methods which are annotated with @RunWithResource
 * will have their resource read and executed as a set of tests. The method itself will not
 * be run currently.
 *
 * Created by ctsims on 10/13/2015.
 */
public class CasePurgeTestRunner extends ParentRunner<CasePurgeTest> {

    private Class<?> testClass;

    public CasePurgeTestRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
        this.testClass = clazz;
    }

    @Override
    protected List<CasePurgeTest> getChildren() {
        List<CasePurgeTest> tests = new ArrayList<>();
        for (Method m : testClass.getMethods()) {
            RunWithResource r = m.getAnnotation(RunWithResource.class);
            if (r != null) {
                for (CasePurgeTest t : CasePurgeTest.getTests(r.value())) {
                    tests.add(t);
                }
            }
        }
        return tests;
    }

    @Override
    protected Description describeChild(CasePurgeTest child) {
        return Description.createTestDescription(CasePurgeTest.class, child.getName());
    }

    @Override
    protected void runChild(CasePurgeTest child, RunNotifier notifier) {
        try {
            notifier.fireTestStarted(describeChild(child));
            child.executeTest();
            notifier.fireTestFinished(describeChild(child));
        } catch (Throwable throwable) {
            notifier.fireTestFailure(new Failure(describeChild(child), throwable));
        }
    }
}