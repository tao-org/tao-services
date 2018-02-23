package ro.cs.tds.test;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ro.cs.tao.services.entity.DataServicesLauncher;
import ro.cs.tao.topology.NodeDescription;

/**
 * Created by cosmin on 11/24/2017.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = DataServicesLauncher.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TopologyServiceIntegrationTest extends AbstractServiceIntegrationTest<NodeDescription> {

    private static final String nodeId = "node08.tao.c-s.ro";
    private static final String description = "Some description";
    private static final String urlMappingStr = "/topology/";

    public TopologyServiceIntegrationTest() {
        super(NodeDescription.class);
    }

    protected String getUrlMappingStr()
    {
        return urlMappingStr;
    }
    protected String getTestItemId()
    {
        return nodeId;
    }

    protected String getTestItemJson()
    {
        return testNodeJson;
    }
    protected boolean checkItem(NodeDescription item, boolean isActive)
    {
        if (nodeId.equals(item.getHostName()) && description.equals(item.getDescription()) &&
                item.getActive() == isActive) {
            return true;
        }
        return false;
    }

    private static final String testNodeJson =
            "{" +
            "    \"hostName\": \"" + nodeId + "\"," +
            "    \"userName\": \"root\"," +
            "    \"userPass\": \"abc123.\"," +
            "    \"processorCount\": 1," +
            "    \"memorySizeGB\": 4," +
            "    \"diskSpaceSizeGB\": 1000," +
            "    \"description\": \"" + description + "\"," +
            "    \"active\": true," +
            "    \"servicesStatus\": []" +
            "}";
}