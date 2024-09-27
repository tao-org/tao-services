package ro.cs.tao.services.commons.update;

import org.springframework.beans.factory.annotation.Autowired;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.configuration.ConfigurationProvider;
import ro.cs.tao.utils.FileUtilities;
import ro.cs.tao.utils.NetStreamResponse;
import ro.cs.tao.utils.NetUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

public class UpdateChecker {
    private static final String REMOVE = "ToRemove";
    private static final String NEW = "New";
    private static final String UPDATE = "Update";
    private static UpdateChecker instance;
    private final Map<String, String> localChecksums;
    private final String remoteUrl;
    private final Logger logger;

    @Autowired
    private RestartService restartService;

    public static void initialize() {
        if (instance == null) {
            final Logger localLogger = Logger.getLogger(UpdateChecker.class.getName());
            final ConfigurationProvider cfgManager = ConfigurationManager.getInstance();
            ShutdownHook.register(cfgManager.getApplicationHome());
            final String url = cfgManager.getValue("update.repository.url");
            if (url == null) {
                localLogger.warning("Update repository url not defined");
                return;
            }
            final int interval = Integer.parseInt(cfgManager.getValue("update.interval", "1440"));
            final boolean atStartup = Boolean.parseBoolean(cfgManager.getValue("update.at.startup", "false"));
            UpdateChecker.instance = new UpdateChecker(url, interval, atStartup);
            localLogger.info(String.format("Checking for updates initialized at %d minutes. First check will be done %s",
                                           interval, atStartup ? "now" : "after " + interval + " minutes"));
        }
    }

    private UpdateChecker(String remoteUrl, int interval, boolean atStartup) {
        this.logger = Logger.getLogger(UpdateChecker.class.getName());
        this.remoteUrl = remoteUrl + "/" + UpdateChecker.this.getClass().getPackage().getImplementationVersion() + "/";
        long frequency = 60000L * interval;
        Timer timer = new Timer("Update check", true);
        timer.scheduleAtFixedRate(new UpdateCheckJob(), atStartup ? 0 : frequency, frequency);
        try {
            Path path = ConfigurationManager.getInstance().getApplicationHome().resolve("update").resolve("checksums.md5");
            this.localChecksums = readChecksums(Files.readAllLines(path));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private Map<String, String> readChecksums(List<String> lines) {
        final Map<String, String> map = new HashMap<>();
        for (String line : lines) {
            int idx = line.lastIndexOf('"');
            if (idx > 0) {
                map.put(line.substring(0, idx).replace("\"", ""),
                        line.substring(idx + 2));
            }
        }
        return map;
    }

    private Map<String, List<String>> computeKeyDifferences(Map<String, String> first, Map<String, String> second) {
        Map<String, List<String>> differences = new HashMap<>();
        differences.put(REMOVE, new ArrayList<>());
        differences.put(NEW, new ArrayList<>());
        differences.put(UPDATE, new ArrayList<>());
        for (Map.Entry<String, String> entry : first.entrySet()) {
            final String secondValue = second.get(entry.getKey());
            if (secondValue != null) {
                if (!entry.getValue().equals(secondValue)) {
                    differences.get(UPDATE).add(entry.getKey());
                }
            } else {
                differences.get(REMOVE).add(entry.getKey());
            }
        }
        second.entrySet().stream().filter(e -> !first.containsKey(e.getKey()))
                         .forEach(e -> differences.get(NEW).add(e.getKey()));
        return differences;
    }

    private Path downloadFile(String fileName, Path targetFolder, Path backupFolder) throws IOException {
        final Path targetFile = targetFolder.resolve(fileName);
        Path backupFile = null;
        if (Files.exists(targetFile)) {
            FileUtilities.createDirectories(backupFolder);
            backupFile = backupFolder.resolve(fileName + ".prev");
            Files.move(targetFile, backupFile);
        }
        final String remoteFile = this.remoteUrl + fileName;
        try (NetStreamResponse responseAsStream = NetUtils.getResponseAsStream(remoteFile)) {
            Files.write(targetFile, responseAsStream.getStream());
        } catch (Exception ex) {
            logger.severe(String.format("Error reading from input stream %s [%s]", remoteFile, ex.getMessage()));
            Files.deleteIfExists(targetFile);
            if (backupFile != null) {
                Files.move(backupFile, targetFile);
            }
        }
        return targetFile;
    }

    private int performUpdates(Map<String, String> remoteFiles) throws IOException {
        int changes = 0;
        final Map<String, List<String>> differences = computeKeyDifferences(this.localChecksums, remoteFiles);
        if (!differences.values().isEmpty()) {
            final Path targetPath = ConfigurationManager.getInstance().getApplicationHome();
            List<String> files = differences.get(UPDATE);
            int updated = 0;
            for (String entry : files) {
                final int index = entry.indexOf('/');
                final String folder = entry.substring(0, index);
                final String fileName = entry.substring(index + 1);
                downloadFile(fileName, targetPath.resolve(folder), targetPath.resolve(folder).resolve("backup"));
                updated++;
            }
            logger.info(String.format("Updated %d jars", updated));
            files = differences.get(NEW);
            changes += updated;
            updated = 0;
            for (String entry : files) {
                final int index = entry.indexOf('/');
                final String folder = entry.substring(0, index);
                final String fileName = entry.substring(index + 1);
                downloadFile(fileName, targetPath.resolve(folder), targetPath.resolve(folder).resolve("backup"));
                updated++;
            }
            logger.info(String.format("Downloaded %d new jars", updated));
            files = differences.get(REMOVE);
            changes += updated;
            updated = 0;
            for (String entry : files) {
                final int index = entry.indexOf('/');
                final String folder = entry.substring(0, index);
                final String fileName = entry.substring(index + 1);
                Files.delete(targetPath.resolve(folder).resolve(fileName));
                updated++;
            }
            changes += updated;
            logger.info(String.format("Removed %d jars", updated));
        }
        return changes;
    }

    private class UpdateCheckJob extends TimerTask {

        @Override
        public void run() {
            final Path targetPath = ConfigurationManager.getInstance().getApplicationHome();
            try {
                Path checksumFile = downloadFile("checksums.md5",
                                                 targetPath.resolve("update").resolve("checksums.md5"),
                                                 targetPath.resolve("update"));
                final Map<String, String> checkSums = readChecksums(Files.readAllLines(checksumFile));
                if (performUpdates(checkSums) > 0) {
                    logger.info("Services modules have been updated. They will be restarted.");
                    restartService.doRestart();
                }
            } catch (IOException ex) {
                logger.severe(String.format("Cannot retrieve update information from %s. Cause: %s",
                                                                                     remoteUrl, ex.getMessage()));
            }
        }
    }
}
