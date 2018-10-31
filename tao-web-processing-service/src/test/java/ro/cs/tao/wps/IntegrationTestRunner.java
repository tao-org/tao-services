package ro.cs.tao.wps;

import static ro.cs.tao.wps.IntegrationTestHelper.getHttpGetResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.runner.notification.*;
import org.junit.runners.*;
import org.junit.runners.model.*;
import ro.cs.tao.utils.StringUtilities;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This IntegrationTestRunner checks the availability of a web service
 * which is needed to run the integration tests in the annotated test calss.
 * Use this runner in combination with {@link NeededService}.
 */
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
        final NeededService neededService = klass.getAnnotation(NeededService.class);

        if (neededService != null && !StringUtilities.isNullOrEmpty(neededService.value())) {
            final String service = neededService.value();
            try {
                final CloseableHttpResponse httpResponse = getHttpGetResponse(service);
                runIntegrationTests = true;
            } catch (IOException e) {
                runIntegrationTests = false;
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("!!   A service needed to run integration tests is not available.");
                System.out.println("!!      Integration test class: ");
                System.out.println("!!           " + klass.getName());
                System.out.println("!!      The service needed:");
                System.out.println("!!           " + service);
                System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
        } else {
            runIntegrationTests = false;
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("!!   Not able to run integration tests for class:");
            System.out.println("!!      " + klass.getName());
            System.out.println("!!   Service Annotation needed! :");
            System.out.println("!!      " + NeededService.class.getName());
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

    /**
     * Use this annotation to define the service URL which must be available to
     * run the integration test.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface NeededService {

        String value();
    }
}
