package ro.cs.tao.services.entity.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.component.Variable;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.services.commons.BaseController;
import ro.cs.tao.services.entity.beans.FetchFilesRequest;
import ro.cs.tao.utils.PublicHttp;
import ro.cs.tao.utils.SafePaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.Semaphore;

@RestController
@RequestMapping("/utilities")
@Tag(name = "Utilities", description = "Bounded remote preview retrieval")
public class UtilityController extends BaseController {
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final Semaphore FETCHES = new Semaphore(4);

    @GetMapping("/tunnel/get")
    public ResponseEntity<?> get(@RequestParam("url") String url, @RequestParam("name") String name,
                                 @RequestParam(name = "user", required = false) String user,
                                 @RequestParam(name = "password", required = false) String password) {
        try { return ResponseEntity.ok(fetch(url, name, user, password)); }
        catch (IOException | IllegalArgumentException e) { return ResponseEntity.badRequest().body("Preview request rejected"); }
    }

    @PostMapping("/tunnel/getmany")
    public ResponseEntity<?> getMany(@RequestBody FetchFilesRequest request) {
        if (request == null || request.getUrls() == null || request.getUrls().isEmpty() || request.getUrls().size() > 20) {
            return ResponseEntity.badRequest().body("Supply between 1 and 20 previews");
        }
        try {
            for (Variable item : request.getUrls()) {
                item.setKey(fetch(item.getKey(), item.getValue(), request.getUser(), request.getPassword()));
            }
            return ResponseEntity.ok(request.getUrls());
        } catch (IOException | IllegalArgumentException e) { return ResponseEntity.badRequest().body("Preview request rejected"); }
    }

    /** Private previews are never exposed through the site's static resource handler. */
    @GetMapping("/preview/{name}")
    public ResponseEntity<?> preview(@PathVariable("name") String name) {
        try {
            SafePaths.fileName(name);
            if (!name.matches("[a-f0-9-]{36}\\.(png|jpg)")) return ResponseEntity.notFound().build();
            Path file = SafePaths.resolve(cacheDirectory(), name);
            if (!Files.isRegularFile(file) || Files.size(file) > MAX_BYTES) return ResponseEntity.notFound().build();
            return ResponseEntity.ok().contentType(name.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
                    .header("Cache-Control", "private, no-store").header("X-Content-Type-Options", "nosniff")
                    .body(Files.readAllBytes(file));
        } catch (IOException | IllegalArgumentException e) { return ResponseEntity.notFound().build(); }
    }

    private String fetch(String url, String name, String user, String password) throws IOException {
        SafePaths.fileName(name);
        Path directory = cacheDirectory();
        if (!FETCHES.tryAcquire()) throw new IOException("Preview capacity exceeded");
        try {
            var credentials = user == null || password == null ? null : new UsernamePasswordCredentials(user, password);
            byte[] data = PublicHttp.get(url, credentials, MAX_BYTES);
            String extension;
            if (data.length >= 8 && data[0] == (byte) 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G'
                    && data[4] == 13 && data[5] == 10 && data[6] == 26 && data[7] == 10) extension = ".png";
            else if (data.length >= 3 && data[0] == (byte) 0xff && data[1] == (byte) 0xd8 && data[2] == (byte) 0xff) extension = ".jpg";
            else throw new IOException("Only PNG and JPEG previews are supported");
            synchronized (UtilityController.class) {
                try (var files = Files.list(directory)) {
                    for (Path file : files.toList()) {
                        if (Files.getLastModifiedTime(file, LinkOption.NOFOLLOW_LINKS).toMillis() < System.currentTimeMillis() - 3600000L) {
                            Files.deleteIfExists(file);
                        }
                    }
                }
                try (var files = Files.list(directory)) {
                    if (files.limit(100).count() >= 100) throw new IOException("Preview quota exceeded");
                }
                String id = UUID.randomUUID() + extension;
                Path file = SafePaths.resolve(directory, id);
                Files.write(file, data, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
                String base = ConfigurationManager.getInstance().getValue("tao.services.base", "");
                return base.replaceAll("/+$", "") + "/utilities/preview/" + id;
            }
        } finally { FETCHES.release(); }
    }

    private Path cacheDirectory() throws IOException {
        String owner = currentOwner();
        if (owner == null || owner.isBlank()) throw new IOException("Authentication required");
        try {
            String id = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(owner.getBytes(StandardCharsets.UTF_8)));
            Path directory = Path.of(System.getProperty("java.io.tmpdir"), "tao-private-previews", id);
            Files.createDirectories(directory);
            return SafePaths.resolve(directory, ".");
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
