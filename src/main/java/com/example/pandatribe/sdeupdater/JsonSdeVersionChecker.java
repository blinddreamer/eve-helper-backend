package com.example.pandatribe.sdeupdater;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Service to check for EVE SDE updates from the official EVE Online developers site.
 * Monitors the JSON format SDE releases by querying the ESI /status/ endpoint for server_version.
 */
@Slf4j
@Service
public class JsonSdeVersionChecker {

    @Value("${eve.sde.tracking.file:data/sde_tracker.properties}")
    private String trackingFile;

    private final Properties trackedVersions = new Properties();
    private final JsonSdeUpdater jsonSdeUpdater;

    // Constructor injection - JsonSdeUpdater is always required
    public JsonSdeVersionChecker(JsonSdeUpdater jsonSdeUpdater) {
        this.jsonSdeUpdater = jsonSdeUpdater;
    }

    @PostConstruct
    public void init() {
        ensureTrackingFileExists();
        loadTrackedVersions();
    }

    /**
     * Checks for SDE updates by comparing the checksum of the latest release
     * with the tracked version. If a new version is detected, triggers the update process.
     */
    public void checkForUpdates() {
        try {
            String currentVersion = getCurrentRemoteVersion();
            String trackedVersion = trackedVersions.getProperty("sde_version");

            if (currentVersion != null && !currentVersion.equals(trackedVersion)) {
                log.info("New SDE version detected: {} (current: {})", currentVersion, trackedVersion);
                createUpdateTrigger(currentVersion);
                jsonSdeUpdater.checkAndUpdate();
                updateTrackedVersion(currentVersion);
            } else {
                log.info("No SDE updates available. Current version: {}", trackedVersion);
            }
        } catch (Exception e) {
            log.error("Error during SDE version check", e);
        }
    }

    /**
     * Gets the current remote SDE version by checking ESI /status/ endpoint
     * which provides the server_version number used in SDE filenames.
     *
     * @return server version string (e.g., "3118350")
     */
    private String getCurrentRemoteVersion() {
        try {
            // Query ESI status endpoint to get current server version
            URL url = new URL("https://esi.evetech.net/latest/status/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream is = connection.getInputStream()) {
                    // Read JSON response
                    StringBuilder response = new StringBuilder();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        response.append(new String(buffer, 0, bytesRead));
                    }

                    // Parse server_version from JSON
                    // Example: {"players":23125,"server_version":"3118350","start_time":"..."}
                    String json = response.toString();
                    String versionKey = "\"server_version\":\"";
                    int startIndex = json.indexOf(versionKey);
                    if (startIndex != -1) {
                        startIndex += versionKey.length();
                        int endIndex = json.indexOf("\"", startIndex);
                        if (endIndex != -1) {
                            String serverVersion = json.substring(startIndex, endIndex);
                            log.debug("SDE server_version from ESI: {}", serverVersion);
                            return serverVersion;
                        }
                    }
                }
            }
            connection.disconnect();
        } catch (IOException e) {
            log.warn("Failed to check SDE version from ESI: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Creates a trigger file indicating that an update is needed
     *
     * @param version the new SDE version identifier
     */
    private void createUpdateTrigger(String version) throws IOException {
        Path triggerFile = Paths.get("data/sde_update_trigger.txt");
        Files.createDirectories(triggerFile.getParent());
        Files.write(triggerFile,
            Collections.singletonList(version),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);
        log.debug("Update trigger created for version: {}", version);
    }

    /**
     * Updates the tracked SDE version in the properties file
     *
     * @param version the new version to track
     */
    private void updateTrackedVersion(String version) {
        trackedVersions.setProperty("sde_version", version);
        trackedVersions.setProperty("last_update", new Date().toString());
        saveTrackedVersions();
    }

    /**
     * Ensures the tracking file exists, creating it if necessary
     */
    private void ensureTrackingFileExists() {
        try {
            Path filePath = Paths.get(trackingFile);
            if (!Files.exists(filePath)) {
                Files.createDirectories(filePath.getParent());
                Files.createFile(filePath);
                log.info("Created SDE tracking file: {}", trackingFile);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create tracking file", e);
        }
    }

    /**
     * Loads tracked SDE versions from the properties file
     */
    private void loadTrackedVersions() {
        try (InputStream input = Files.newInputStream(Paths.get(trackingFile))) {
            trackedVersions.load(input);
            log.debug("Loaded tracked SDE versions");
        } catch (IOException e) {
            log.warn("Could not load tracked versions, starting fresh", e);
        }
    }

    /**
     * Saves tracked SDE versions to the properties file
     */
    private void saveTrackedVersions() {
        try (OutputStream output = Files.newOutputStream(Paths.get(trackingFile))) {
            trackedVersions.store(output, "EVE SDE Version Tracker - JSON Format");
            log.debug("Saved tracked SDE versions");
        } catch (IOException e) {
            log.error("Could not save tracked versions", e);
        }
    }
}
