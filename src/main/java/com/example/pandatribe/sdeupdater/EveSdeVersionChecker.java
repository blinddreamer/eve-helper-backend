package com.example.pandatribe.sdeupdater;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EveSdeVersionChecker {

    @Value("${eve.sde.fuzzwork.url:https://www.fuzzwork.co.uk/dump/latest/}")
    private String fuzzworkUrl;

    @Value("${eve.sde.tracking.file:data/sde_tracker.properties}")
    private String trackingFile;

    private static final List<String> SQL_FILES_TO_DOWNLOAD = Arrays.asList(
            "industryActivityMaterials.sql.bz2",
            "industryActivityProducts.sql.bz2",
            "invTypes.sql.bz2",
            "invVolumes.sql.bz2",
            "mapRegions.sql.bz2",
            "mapSolarSystems.sql.bz2",
            "staStations.sql.bz2",
            "planetSchematics.sql.bz2",
            "planetSchematicsTypeMap.sql.bz2"
    );

    private final Properties trackedVersions = new Properties();

    private final EveSdeUpdater eveSdeUpdater;

    public EveSdeVersionChecker(EveSdeUpdater eveSdeUpdater) {
        this.eveSdeUpdater = eveSdeUpdater;
    }

    @PostConstruct
    public void init() {
        ensureTrackingFileExists();
        loadTrackedVersions();
    }


    public void checkForUpdates() {
        try {
            Map<String, String> currentVersions = getCurrentRemoteVersions();
            List<String> updatedFiles = findUpdatedFiles(currentVersions);

            if (!updatedFiles.isEmpty()) {
                log.info("New SDE updates available for files: {}", updatedFiles);
                createUpdateTrigger(updatedFiles);
                eveSdeUpdater.checkAndUpdate();
            } else {
                log.info("No SDE updates available");
            }
        } catch (Exception e) {
            log.error("Error during SDE version check", e);
        }
    }

    private Map<String, String> getCurrentRemoteVersions(){
        Map<String, String> versions = new HashMap<>();

        for (String filename : SQL_FILES_TO_DOWNLOAD) {
            try {
                URL url = new URL(fuzzworkUrl + filename);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    String lastModified = connection.getHeaderField("Last-Modified");
                    String contentLength = connection.getHeaderField("Content-Length");
                    String etag = connection.getHeaderField("ETag");

                    // Create a composite version identifier
                    String versionId = String.format("%s|%s|%s",
                            lastModified != null ? lastModified : "",
                            contentLength != null ? contentLength : "",
                            etag != null ? etag : "");

                    if (!versionId.trim().isEmpty()) {
                        versions.put(filename, DigestUtils.sha256Hex(versionId));
                    }
                }
                connection.disconnect();
            } catch (IOException e) {
                log.warn("Failed to check version for {}: {}", filename, e.getMessage());
            }
        }

        return versions;
    }

    private List<String> findUpdatedFiles(Map<String, String> currentVersions) {
        return currentVersions.entrySet().stream()
                .filter(entry -> {
                    String trackedVersion = trackedVersions.getProperty(entry.getKey());
                    return trackedVersion == null || !trackedVersion.equals(entry.getValue());
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @SneakyThrows
    private void createUpdateTrigger(List<String> updatedFiles) {
        // Create a trigger file with the files that need updating
        Path triggerFile = Paths.get("data/sde_update_trigger.txt");
        Files.write(triggerFile, updatedFiles, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // Update tracked versions
        updatedFiles.forEach(file ->
                trackedVersions.setProperty(file, getCurrentRemoteVersions().get(file)));
        saveTrackedVersions();
    }

    private void ensureTrackingFileExists() {
        try {
            Path filePath = Paths.get(trackingFile);
            if (!Files.exists(filePath)) {
                Files.createDirectories(filePath.getParent());
                Files.createFile(filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create tracking file", e);
        }
    }

    private void loadTrackedVersions() {
        try (InputStream input = Files.newInputStream(Paths.get(trackingFile))) {
            trackedVersions.load(input);
        } catch (IOException e) {
            log.warn("Could not load tracked versions", e);
        }
    }

    private void saveTrackedVersions() {
        try (OutputStream output = Files.newOutputStream(Paths.get(trackingFile))) {
            trackedVersions.store(output, "EVE SDE Version Tracker");
        } catch (IOException e) {
            log.error("Could not save tracked versions", e);
        }
    }
}
