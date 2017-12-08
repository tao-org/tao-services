package ro.cs.tds.test;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ro.cs.tao.docker.Container;
import ro.cs.tao.services.app.ComponentApplication;

/**
 * Created by cosmin on 11/24/2017.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ComponentApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ContainerServiceIntegrationTest extends AbstractServiceIntegrationTest<Container> {

    private static final String containerId = "DummyTestDockerContainerTest";
    private static final String containerName = "DummyTestDockerContainerTest";
    private static final String urlMappingStr = "/docker/";

    public ContainerServiceIntegrationTest() {
        super(Container.class);
    }

    protected String getUrlMappingStr()
    {
        return urlMappingStr;
    }
    protected String getTestItemId()
    {
        return containerId;
    }

    protected String getTestItemJson()
    {
        return testComponentJson;
    }
    protected boolean checkItem(Container item, boolean isActive)
    {
        if (containerId.equals(item.getId()) && containerName.equals(item.getName())) {
            return true;
        }
        return false;
    }

    private static final String testComponentJson =
            "{" +
            "    \"id\": \"" + containerId + "\"," +
            "    \"name\": \"" + containerName + "\"," +
            "    \"tag\": \"some dummy tag\"," +
            "    \"applicationPath\": \"/usr/bin/\"," +
            "    \"applications\": [{" +
                    "    \"path\": \"Some path\"," +
                    "    \"name\": \"someName\"" +
                    "}]" +
            "}";
}