package ro.cs.tao.wps;

import static ro.cs.tao.wps.IntegrationTestHelper.getHttpGetResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.runner.notification.*;
import org.junit.runners.*;
import org.junit.runners.model.*;

import java.io.IOException;

public class IntegrationTestRunner extends BlockJUnit4ClassRunner {

    boolean runIntegrationTests;

    /**
     * Creates a BlockJUnit4ClassRunner to run {@code klass}
     *
     * @param klass
     *
     * @throws InitializationError if the test class is malformed.
     */
    public IntegrationTestRunner(Class<?> klass) throws InitializationError {
        super(klass);

        final String service = "http://localhost:8080/wps";
        try {
            final CloseableHttpResponse httpResponse = getHttpGetResponse(service);
            runIntegrationTests = true;
        } catch (IOException e) {
            runIntegrationTests = false;
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("!!   The service needed for this integration test is not available.");
            System.out.println("!!      Integration test class: ");
            System.out.println("!!           " + klass.getName());
            System.out.println("!!      The needed service:");
            System.out.println("!!           " + service);
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        if (runIntegrationTests) {
            super.runChild(method, notifier);
        } else {
            notifier.fireTestIgnored(describeChild(method));
        }
    }
}
