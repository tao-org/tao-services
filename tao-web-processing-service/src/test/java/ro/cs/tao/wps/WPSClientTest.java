package ro.cs.tao.wps;

import org.geotools.ows.ServiceException;
import ro.cs.tao.component.WPSAuthentication;
import ro.cs.tao.component.enums.AuthenticationType;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.docker.Container;
import ro.cs.tao.services.interfaces.WebProcessingService;
import ro.cs.tao.wps.impl.WPSClient;

import java.io.IOException;

public class WPSClientTest {

    public static void main(String[] args) throws ServiceException, IOException {
        ConfigurationManager.getInstance().setValue("workspace.location", "/");
        WPSAuthentication auth = new WPSAuthentication();
        auth.setLoginUrl("https://localhost:8443/login");
        auth.setAuthHeader("X-Auth-Token");
        auth.setUser("admin");
        auth.setPassword("eodata");
        auth.setType(AuthenticationType.TOKEN);
        WPSClient client = new WPSClient("https://localhost:8443/wps", auth, () -> "test");
        final Container capabilities = client.getCapabilities();
        final WebProcessingService.ProcessInfo processInfo = client.describeProcess("23");

        System.exit(0);
    }
}
