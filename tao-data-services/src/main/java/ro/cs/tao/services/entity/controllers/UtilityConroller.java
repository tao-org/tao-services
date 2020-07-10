package ro.cs.tao.services.entity.controllers;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ro.cs.tao.component.Variable;
import ro.cs.tao.configuration.TaoConfigurationProvider;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.entity.beans.FetchFilesRequest;
import ro.cs.tao.utils.NetStreamResponse;
import ro.cs.tao.utils.NetUtils;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.utils.async.Parallel;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/utilities")
public class UtilityConroller extends BaseController {

    @RequestMapping(value = "/tunnel/get", method = RequestMethod.GET)
    public ResponseEntity<?> get(@RequestParam("url") String url,
                                 @RequestParam("name") String productName,
                                 @RequestParam(name = "user", required = false) Optional<String> user,
                                 @RequestParam(name = "password", required = false) Optional<String> password) {
        ResponseEntity<?> serviceResponse;
        try {
            String sitePath = TaoConfigurationProvider.getInstance().getValue("site.path");
            if (sitePath == null || sitePath.isEmpty()) {
                throw new IOException("Cannot determine site path");
            }
            Path cachePath = Paths.get(sitePath).resolve("previews");
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
            }
            Path file = getFile(cachePath, url, productName, user.orElse(null), password.orElse(null));
            if (file == null) {
                throw new IOException("File could not be downloaded");
            }
            serviceResponse = ResponseEntity.ok(Paths.get(sitePath).relativize(file).toString().replace('\\', '/'));
        } catch (IOException e) {
            serviceResponse = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return serviceResponse;
    }

    @RequestMapping(value = "/tunnel/getmany", method = RequestMethod.POST)
    public ResponseEntity<?> getMany(@RequestBody FetchFilesRequest request) {
        ResponseEntity<?> serviceResponse;
        try {
            String sitePath = TaoConfigurationProvider.getInstance().getValue("site.path");
            if (sitePath == null || sitePath.isEmpty()) {
                throw new IOException("Cannot determine site path");
            }
            Path cachePath = Paths.get(sitePath).resolve("previews");
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
            }
            final String user = request.getUser();
            final String password = request.getPassword();
            final List<Variable> urls = request.getUrls();
            if (urls == null || urls.size() == 0) {
                throw new IOException("Empty list");
            }
            final int size = urls.size();
            Parallel.For(1, size, (i) -> {
                                     try {
                                         Path file = getFile(cachePath, urls.get(i).getKey(), urls.get(i).getValue(), user, password);
                                         if (file != null) {
                                             urls.get(i).setKey(Paths.get(sitePath).relativize(file).toString().replace('\\', '/'));
                                         } else {
                                             urls.get(i).setKey("undefined");
                                         }
                                     } catch (Exception e) {
                                         error(e.getMessage());
                                     }
                                 });
            serviceResponse = ResponseEntity.ok(urls);
        } catch (IOException e) {
            serviceResponse = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return serviceResponse;
    }

    private Path getFile(Path cachePath, String url, String productName, String userName, String pwd) throws IOException {
        Path file = Files.list(cachePath).filter(f -> f.getFileName().toString().startsWith(productName)).findFirst().orElse(null);
        if (file == null) {
            URL checkUrl = new URL(url);
            Credentials credentials = null;
            if (!StringUtilities.isNullOrEmpty(userName) && !StringUtilities.isNullOrEmpty(pwd)) {
                credentials = new UsernamePasswordCredentials(userName, pwd);
            }
            NetStreamResponse response = NetUtils.getResponseAsStream(url, credentials);
            String extension = response.getName() != null ?
                    response.getName().substring(response.getName().lastIndexOf('.')) : ".jpg";
            file = cachePath.resolve(productName + extension);
            Files.write(file, response.getStream(), StandardOpenOption.CREATE);
        }
        return file;
    }
}
