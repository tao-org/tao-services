package ro.cs.tao.services.entity.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.component.Variable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.entity.beans.FetchFilesRequest;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.NetStreamResponse;
import ro.cs.tao.utils.NetUtils;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.utils.async.Parallel;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/utilities")
@Tag(name = "Utilities", description = "Request proxy utility controller")
public class UtilityController extends BaseController {

    /**
     * Handles HTTP GET requests to retrieve a file based on the provided parameters.
     * The method processes the request, validates the input, and attempts to fetch or
     * download a file from the specified URL in the request parameters. The response
     * contains the relative path to the downloaded file or an error message if the
     * operation fails.
     *
     * @param url          The URL of the file to be downloaded. This parameter is mandatory.
     * @param productName  The name of the product related to the file being retrieved. This parameter is mandatory.
     * @param user         The username to access the URL, if required. This parameter is optional.
     * @param password     The password to access the URL, if required. This parameter is optional.
     * @return A {@code ResponseEntity} containing the relative path of the downloaded file upon
     *         success or an error message with an HTTP status code of {@code INTERNAL_SERVER_ERROR}
     *         in case of failure.
     */
    @RequestMapping(value = "/tunnel/get", method = RequestMethod.GET)
    public ResponseEntity<?> get(@RequestParam("url") String url,
                                 @RequestParam("name") String productName,
                                 @RequestParam(name = "user", required = false) String user,
                                 @RequestParam(name = "password", required = false) String password) {
        ResponseEntity<?> serviceResponse;
        try {
            if (StringUtilities.isNullOrEmpty(url) || "undefined".equalsIgnoreCase(url)) {
                throw new IOException("Invalid url");
            }
            URI uri = URI.create(url);
            String sitePath = ConfigurationManager.getInstance().getValue("site.path");
            if (sitePath == null || sitePath.isEmpty()) {
                throw new IOException("Cannot determine site path");
            }
            Path cachePath = Paths.get(sitePath).resolve("previews");
            if (!Files.exists(cachePath)) {
                FileUtilities.createDirectories(cachePath);
            }
            Path file = getFile(cachePath, url, productName, user, password);
            if (file == null) {
                throw new IOException("File could not be downloaded");
            }
            serviceResponse = ResponseEntity.ok(Paths.get(sitePath).relativize(file).toString().replace('\\', '/'));
        } catch (Exception e) {
            serviceResponse = new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return serviceResponse;
    }

    /**
     * Handles HTTP POST requests to process a batch of file retrieval requests.
     * The method processes the list of URLs provided in the request, attempts to fetch or generate
     * files associated with the specified parameters, and returns the resulting file paths.
     *
     * @param request The {@code FetchFilesRequest} object containing the following:
     *                - {@code user}: The username to access the URLs, if required.
     *                - {@code password}: The password to access the URLs, if required.
     *                - {@code urls}: A list of {@code Variable} objects, where each contains the URL
     *                  and associated product name to process.
     * @return A {@code ResponseEntity} containing:
     *         - The list of processed URLs with updated file paths upon success.
     *         - An error message with an HTTP status code of {@code INTERNAL_SERVER_ERROR}
     *           in case of processing failure (e.g., invalid configuration or empty URL list).
     */
    @RequestMapping(value = "/tunnel/getmany", method = RequestMethod.POST)
    public ResponseEntity<?> getMany(@RequestBody FetchFilesRequest request) {
        ResponseEntity<?> serviceResponse;
        try {
            String sitePath = ConfigurationManager.getInstance().getValue("site.path");
            if (sitePath == null || sitePath.isEmpty()) {
                throw new IOException("Cannot determine site path");
            }
            Path cachePath = Paths.get(sitePath).resolve("previews");
            if (!Files.exists(cachePath)) {
                FileUtilities.createDirectories(cachePath);
            }
            final String user = request.getUser();
            final String password = request.getPassword();
            final List<Variable> urls = request.getUrls();
            if (urls == null || urls.isEmpty()) {
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
        Path file;
        try (Stream<Path> stream = Files.list(cachePath)) {
            file = stream.filter(f -> f.getFileName().toString().startsWith(productName)).findFirst().orElse(null);
            if (file == null) {
                Credentials credentials = null;
                if (!StringUtilities.isNullOrEmpty(userName) && !StringUtilities.isNullOrEmpty(pwd)) {
                    credentials = new UsernamePasswordCredentials(userName, pwd);
                }
                try (NetStreamResponse response = NetUtils.getResponseAsStream(url, credentials)) {
                    String extension = response.getName() != null ?
                                       response.getName().substring(response.getName().lastIndexOf('.')) : ".jpg";
                    file = cachePath.resolve(productName + extension);
                    Files.write(file, response.getStream(), StandardOpenOption.CREATE);
                }
            }
        }
        return file;
    }
}
