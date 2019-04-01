package ro.cs.tao.services.entity.controllers;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.util.NetStreamResponse;
import ro.cs.tao.datasource.util.NetUtils;
import ro.cs.tao.utils.StringUtilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

@Controller
@RequestMapping("/utilities")
public class UtilityConroller {

    @RequestMapping(value = "/tunnel/get", method = RequestMethod.GET)
    public ResponseEntity<?> get(@RequestParam("url") String url,
                                 @RequestParam("name") String productName,
                                 @RequestParam(name = "user", required = false) Optional<String> user,
                                 @RequestParam(name = "password", required = false) Optional<String> password) {
        ResponseEntity<?> serviceResponse;
        try {
            String sitePath = ConfigurationManager.getInstance().getValue("site.path");
            if (sitePath == null || sitePath.isEmpty()) {
                throw new IOException("Cannot determine site path");
            }
            Path cachePath = Paths.get(sitePath).resolve("previews");
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
            }
            Path file = Files.list(cachePath).filter(f -> f.getFileName().toString().startsWith(productName)).findFirst().orElse(null);
            if (file == null) {
                Credentials credentials = null;
                String userName = null, pwd = null;
                if (user.isPresent()) {
                    userName = user.get();
                }
                if (password.isPresent()) {
                    pwd = password.get();
                }
                if (!StringUtilities.isNullOrEmpty(userName) && !StringUtilities.isNullOrEmpty(pwd)) {
                    credentials = new UsernamePasswordCredentials(userName, pwd);
                }
                NetStreamResponse response = NetUtils.getResponseAsStream(url, credentials);
                String extension = response.getName() != null ?
                        response.getName().substring(response.getName().lastIndexOf('.')) : ".jpg";
                file = cachePath.resolve(productName + extension);
                Files.write(file, response.getStream(), StandardOpenOption.CREATE);
            }
            serviceResponse = ResponseEntity.ok(Paths.get(sitePath).relativize(file).toString().replace('\\', '/'));
        } catch (IOException e) {
            serviceResponse = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return serviceResponse;
    }
}
