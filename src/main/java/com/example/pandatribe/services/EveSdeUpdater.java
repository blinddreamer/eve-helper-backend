package com.example.pandatribe.services;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.io.BufferedReader;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class EveSdeUpdater {
    @PersistenceContext
    private EntityManager entityManager;
    // Configuration
    private static final String FUZZWORK_URL = "https://www.fuzzwork.co.uk/dump/latest/";
    private static final String TEMP_DIR = "temp_sde_downloads";
    private static final String LAST_UPDATE_FILE = "last_update.txt";
    private static final String FILE_TIMESTAMPS_FILE = "file_timestamps.properties";
    private final DateTimeFormatter httpDateFormatter =
            DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.of("GMT"));
    private final Properties fileTimestamps = new Properties();

    // Files to download and execute
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

    @PostConstruct
    public void init() {
        ensureDirectoryExists();
        loadFileTimestamps();
    }

    public void runUpdate() {
        try {
            if (checkForUpdates()) {
                log.info("New SDE updates available. Starting update process...");
                downloadUpdatedFiles();
                processFiles();
                updateLastUpdateTime();
                log.info("SDE update completed successfully!");
            } else {
                log.info("No updates available. Current database is up to date.");
            }
        } catch (Exception e) {
            log.error("Error during EVE SDE update", e);
        } finally {
            cleanupTempFiles();
        }
    }

    private boolean checkForUpdates() throws IOException {
        // Always update if we don't have a last update file
        if (!Files.exists(Paths.get(LAST_UPDATE_FILE))) {
            log.info("No previous update detected - performing initial update");
            return true;
        }

        boolean updatesAvailable = false;

        for (String filename : SQL_FILES_TO_DOWNLOAD) {
            try {
                Instant remoteLastModified = getRemoteLastModified(filename);
                Instant localLastModified = getLocalLastModified(filename);

                if (remoteLastModified == null) {
                    log.warn("Could not determine last modified date for {}", filename);
                    continue;
                }

                if(localLastModified == null || remoteLastModified.equals(localLastModified)){
                    log.info("No newer versions are available");
                    break;
                }

                if (remoteLastModified.isAfter(localLastModified)) {
                    log.info("Update available for {} (remote: {}, local: {})",
                            filename, remoteLastModified, localLastModified);
                    updatesAvailable = true;
                    break;
                }
            } catch (IOException e) {
                log.error("Error checking update status for {}", filename, e);
                throw e;
            }
        }

        return updatesAvailable;
    }

    private Instant getRemoteLastModified(String filename) throws IOException {
        URL url = new URL(FUZZWORK_URL + filename);
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(10));
            connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(30));

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.warn("HTTP {} when checking {}", responseCode, filename);
                return null;
            }

            String lastModifiedHeader = connection.getHeaderField("Last-Modified");
            if (lastModifiedHeader != null) {
                try {
                    return httpDateFormatter.parse(lastModifiedHeader, Instant::from);
                } catch (DateTimeParseException e) {
                    log.warn("Invalid Last-Modified header for {}: {}", filename, lastModifiedHeader);
                }
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    private Instant getLocalLastModified(String filename) {
        String timestamp = fileTimestamps.getProperty(filename);
        return timestamp != null ? Instant.parse(timestamp) : null;
    }

    @Retryable(value = {IOException.class}, maxAttempts = 3, backoff = @Backoff(delay = 5000))
    private void downloadUpdatedFiles() throws IOException {
        for (String filename : SQL_FILES_TO_DOWNLOAD) {
            Instant remoteLastModified = getRemoteLastModified(filename);
            Instant localLastModified = getLocalLastModified(filename);

            if (remoteLastModified != null &&
                    (localLastModified == null || remoteLastModified.isAfter(localLastModified))) {

                downloadFile(filename, remoteLastModified);
            }
        }
        saveFileTimestamps();
    }

    private void downloadFile(String filename, Instant lastModified) throws IOException {
        URL url = new URL(FUZZWORK_URL + filename);
        Path target = Paths.get(TEMP_DIR, filename);

        log.info("Downloading {}...", filename);
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(30));
            connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(60));

            try (InputStream in = connection.getInputStream();
                 OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }

                // Store the last modified timestamp
                fileTimestamps.setProperty(filename, lastModified.toString());
                log.info("Successfully downloaded {} ({} bytes)", filename, Files.size(target));
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void processFiles() throws IOException {
        for (String filename : SQL_FILES_TO_DOWNLOAD) {
            Path compressedFile = Paths.get(TEMP_DIR, filename);

            if (Files.exists(compressedFile) && filename.endsWith(".sql.bz2")) {
                log.info("Processing file: {}", filename);
                Path decompressedFile = decompressBz2(compressedFile);
                try {
                    executeSqlWithEntityManager(decompressedFile);
                } finally {
                    Files.deleteIfExists(decompressedFile);
                }
            }
        }
    }

    private Path decompressBz2(Path inputFile) throws IOException {
        String outputFilename = inputFile.getFileName().toString().replace(".bz2", "");
        Path outputFile = Paths.get(TEMP_DIR, outputFilename);

        log.info("Decompressing {} to {}", inputFile, outputFile);

        try (InputStream fin = Files.newInputStream(inputFile);
             BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(fin);
             OutputStream fout = Files.newOutputStream(outputFile)) {

            byte[] buffer = new byte[8192];
            int n;
            while ((n = bzIn.read(buffer)) != -1) {
                fout.write(buffer, 0, n);
            }
        }

        return outputFile;
    }


    protected void executeSqlWithEntityManager(Path sqlFile) throws IOException {
        log.info("Executing SQL file: {}", sqlFile.getFileName());

        try (BufferedReader reader = Files.newBufferedReader(sqlFile, StandardCharsets.UTF_8)) {
            String line;
            StringBuilder statementBuilder = new StringBuilder();
            int statementCount = 0;
            long startTime = System.currentTimeMillis();

            while ((line = reader.readLine()) != null) {
                // Skip comments and empty lines
                if (line.startsWith("--") || line.trim().isEmpty()) {
                    continue;
                }

                statementBuilder.append(line).append("\n");

                // Check for end of statement
                if (line.trim().endsWith(";")) {
                    String sql = statementBuilder.toString().trim();
                    sql = sql.substring(0, sql.length() - 1); // Remove trailing semicolon

                    try {
                        // Explicitly join transaction
                        entityManager.joinTransaction();
                        int updateCount = entityManager.createNativeQuery(sql).executeUpdate();
                        statementCount++;

                        if (statementCount % 100 == 0) {
                            entityManager.flush();
                            // Don't clear() as it can detach transactional entities
                            log.debug("Processed {} statements (last update count: {})",
                                    statementCount, updateCount);
                        }
                    } catch (Exception e) {
                        log.error("Error executing statement {} ({} chars):\n{}",
                                statementCount,
                                sql.length(),
                                sql.substring(0, Math.min(200, sql.length())),
                                e);
                        // Explicitly mark for rollback
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                        throw e;
                    }
                    statementBuilder = new StringBuilder();
                }
            }

            entityManager.flush();
            long duration = System.currentTimeMillis() - startTime;
            log.info("Executed {} statements from {} in {} ms",
                    statementCount, sqlFile.getFileName(), duration);
        }
    }

    private void loadFileTimestamps() {
        Path timestampFile = Paths.get(TEMP_DIR, FILE_TIMESTAMPS_FILE);
        if (Files.exists(timestampFile)) {
            try (InputStream in = Files.newInputStream(timestampFile)) {
                fileTimestamps.load(in);
                log.info("Loaded {} file timestamps", fileTimestamps.size());
            } catch (IOException e) {
                log.warn("Failed to load file timestamps", e);
            }
        }
    }

    private void saveFileTimestamps() {
        Path timestampFile = Paths.get(TEMP_DIR, FILE_TIMESTAMPS_FILE);
        try (OutputStream out = Files.newOutputStream(timestampFile,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            fileTimestamps.store(out, "File modification timestamps");
            log.debug("Saved {} file timestamps", fileTimestamps.size());
        } catch (IOException e) {
            log.warn("Failed to save file timestamps", e);
        }
    }

    private void updateLastUpdateTime() throws IOException {
        String timestamp = Instant.now().toString();
        Files.write(Paths.get(LAST_UPDATE_FILE),
                timestamp.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        log.debug("Updated last update timestamp to {}", timestamp);
    }

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(Paths.get(TEMP_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Could not create temp directory", e);
        }
    }

    private void cleanupTempFiles() {
        try {
            Path tempDir = Paths.get(TEMP_DIR);
            if (Files.exists(tempDir)) {
                log.debug("Cleaning up temporary files in {}", TEMP_DIR);

                // First delete all files in the directory
                Files.walk(tempDir)
                        .filter(path -> !path.equals(tempDir)) // Don't try to delete the directory itself yet
                        .sorted(Comparator.reverseOrder()) // Delete files before their parent directories
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                                log.trace("Deleted temp file: {}", path);
                            } catch (IOException e) {
                                log.warn("Failed to delete temp file: {}", path, e);
                            }
                        });

                // Now try to delete the directory itself
                try {
                    Files.deleteIfExists(tempDir);
                    log.trace("Deleted temp directory: {}", TEMP_DIR);
                } catch (IOException e) {
                    log.debug("Could not delete temp directory (may not be empty): {}", TEMP_DIR);
                }
            }
        } catch (IOException e) {
            log.warn("Error during temp files cleanup", e);
        }
    }
}

