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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to check for EVE SDE updates from the official EVE Online developers site.
 * Monitors the JSON format SDE releases via the latest.jsonl endpoint which reflects
 * the build number of the currently published SDE ZIP.
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
     * Discovers the current published SDE version from the official CCP latest.jsonl endpoint.
     * This endpoint returns a JSONL record containing the key "sde" with the current build number.
     *
     * Using this endpoint instead of the ESI server_version because:
     * - ESI server_version changes with every patch/hotfix
     * - CCP only publishes a new SDE ZIP with major releases
     * - This endpoint always reflects what is actually downloadable
     *
     * @return SDE build number string (e.g. "3350000"), or null if the endpoint is unreachable
     */
    private String getCurrentRemoteVersion() {
        try {
            URL url = new URL("https://developers.eveonline.com/static-data/tranquility/latest.jsonl");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            connection.setRequestProperty("Accept", "application/json, application/x-ndjson, */*");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String body = readStream(connection.getInputStream());
                // Record format: {"_key":"sde","buildNumber":3351823,"releaseDate":"..."}
                Matcher matcher = Pattern.compile("\"buildNumber\"\\s*:\\s*(\\d+)").matcher(body);
                if (matcher.find()) {
                    String version = matcher.group(1);
                    log.debug("Latest published SDE version from latest.jsonl: {}", version);
                    return version;
                }
                log.warn("Could not find 'sde' key in latest.jsonl response: {}", body);
            } else {
                log.warn("latest.jsonl endpoint returned HTTP {} — cannot determine SDE version", responseCode);
            }
            connection.disconnect();
        } catch (IOException e) {
            log.warn("Failed to fetch SDE version from latest.jsonl: {}", e.getMessage());
        }
        return null;
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
        }
        return sb.toString();
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
