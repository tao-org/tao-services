package ro.cs.tao.wps;

import org.geotools.ows.ServiceException;
import ro.cs.tao.component.WebServiceAuthentication;
import ro.cs.tao.component.enums.AuthenticationType;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.docker.Container;
import ro.cs.tao.services.interfaces.WebProcessingService;
import ro.cs.tao.wps.impl.WPSClient;

import java.io.IOException;

public class WPSClientTest {

    public static void main(String[] args) throws ServiceException, IOException {
        ConfigurationManager.getInstance().setValue("workspace.location", "/");
        WebServiceAuthentication auth = new WebServiceAuthentication();
        auth.setLoginUrl("http://192.168.61.18:8280/login");
        auth.setAuthHeader("X-Auth-Token");
        auth.setUser("test");
        auth.setPassword("test123");
        auth.setType(AuthenticationType.TOKEN);
        WPSClient client = new WPSClient("http://192.168.61.18:8280/wps", auth, () -> "test");
        final Container capabilities = client.getCapabilities();
        final WebProcessingService.ProcessInfo processInfo = client.describeProcess("23");

        System.exit(0);
    }
}
