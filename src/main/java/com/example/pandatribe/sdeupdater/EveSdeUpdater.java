package com.example.pandatribe.sdeupdater;

import com.example.pandatribe.feign.contracts.EveInteractor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EveSdeUpdater {

    // Configuration properties with defaults
    @Value("${eve.sde.fuzzwork.url:https://www.fuzzwork.co.uk/dump/latest/}")
    private String fuzzworkUrl;

    @Value("${eve.sde.temp.dir:temp_sde_downloads}")
    private String tempDir;

    @Value("${eve.sde.flyway.locations:filesystem:db/sde_migrations}")
    private String flywayLocations;

    @Value("${eve.sde.flyway.table:sde_schema_history}")
    private String flywayTable;

    private final EveInteractor eveInteractor;

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

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void init() {
        ensureDirectoryExists();
        ensureFlywayDirectoryExists();
    }

    // Main scheduled method to check for updates and process them

    public void checkAndUpdate() {
        try {
            if (updateTriggerExists()) {
                List<String> filesToUpdate = readUpdateTrigger();
                if (!filesToUpdate.isEmpty()) {
                    log.info("Processing SDE updates for files: {}", filesToUpdate);
                    downloadFiles(filesToUpdate);
                    decompressFiles(filesToUpdate);
                    prepareFlywayScripts(filesToUpdate);
                    runFlywayMigration();
                    clearUpdateTrigger();
                    log.info("SDE update completed successfully");
                }
            }
        } catch (Exception e) {
            log.error("Error during SDE update process", e);
        } finally {
            cleanupTempFiles();
        }
    }

    // File management methods
    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(Paths.get(tempDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create temp directory: " + tempDir, e);
        }
    }

    private void ensureFlywayDirectoryExists() {
        String locationPath = flywayLocations.split(",")[0]
                .replace("filesystem:", "")
                .trim();
        try {
            Files.createDirectories(Paths.get(locationPath));
        } catch (IOException e) {
            throw new RuntimeException("Could not create Flyway directory: " + locationPath, e);
        }
    }

    private boolean updateTriggerExists() {
        return Files.exists(Paths.get("data/sde_update_trigger.txt"));
    }

    private List<String> readUpdateTrigger() throws IOException {
        return Files.readAllLines(Paths.get("data/sde_update_trigger.txt"));
    }

    private void clearUpdateTrigger() throws IOException {
        Files.deleteIfExists(Paths.get("data/sde_update_trigger.txt"));
    }

    // Download and file processing methods
    private void downloadFiles(List<String> filenames) throws IOException {
        // Ensure target directory exists
        Files.createDirectories(Paths.get(tempDir));

        for (String filename : filenames) {
            if (!SQL_FILES_TO_DOWNLOAD.contains(filename)) {
                log.warn("Skipping download of non-whitelisted file: {}", filename);
                continue;
            }

            URL url = new URL(fuzzworkUrl + filename);
            Path target = Paths.get(tempDir, filename);

            log.info("Downloading {}...", filename);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(30000); // 30 seconds
            connection.setReadTimeout(60000);    // 60 seconds

            try (InputStream in = connection.getInputStream();
                 OutputStream out = Files.newOutputStream(
                         target,
                         StandardOpenOption.CREATE,
                         StandardOpenOption.TRUNCATE_EXISTING,
                         StandardOpenOption.WRITE)) {

                // Use a buffer for efficient transfer
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }

                log.debug("Downloaded {} ({} bytes)", filename, totalBytes);
            } catch (IOException e) {
                // Delete partially downloaded file if error occurs
                Files.deleteIfExists(target);
                throw new IOException("Failed to download file: " + filename, e);
            } finally {
                connection.disconnect();
            }
        }
    }

    private void decompressFiles(List<String> filenames) throws IOException {
        for (String filename : filenames) {
            if (filename.endsWith(".bz2")) {
                Path compressedFile = Paths.get(tempDir, filename);
                Path decompressedFile = Paths.get(tempDir, filename.replace(".bz2", ""));

                log.info("Decompressing {} to {}", filename, decompressedFile.getFileName());
                try (InputStream fileIn = Files.newInputStream(compressedFile);
                     BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(fileIn);
                     OutputStream fileOut = Files.newOutputStream(decompressedFile)) {

                    byte[] buffer = new byte[8192];
                    int n;
                    while ((n = bzIn.read(buffer)) != -1) {
                        fileOut.write(buffer, 0, n);
                    }
                }
                // Delete the compressed file after decompression
                Files.deleteIfExists(compressedFile);
            }
        }
    }

    // Flyway integration methods
    private void prepareFlywayScripts(List<String> filenames) throws IOException {
        // Clear existing repeatable migrations
        String primaryLocation = flywayLocations.split(",")[0].replace("filesystem:", "").trim();
        Path flywayDir = Paths.get(primaryLocation);

        Files.walk(flywayDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().startsWith("R__"))
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        log.warn("Could not delete old migration file: {}", p, e);
                    }
                });

        // Prepare new properly named repeatable migration files
        for (String filename : filenames) {
            String sqlFilename = filename.replace(".bz2", "");
            if (!sqlFilename.endsWith(".sql")) {
                log.warn("Skipping non-SQL file: {}", sqlFilename);
                continue;
            }
            Path sourceFile = Paths.get(tempDir, sqlFilename);
            if (Files.exists(sourceFile)) {
                // Ensure proper Flyway naming convention: R__description.sql
                String flywayFilename = "R__import_" + sqlFilename.replace(".sql", "") + ".sql";
                Path targetFile = flywayDir.resolve(flywayFilename);

                Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                log.info("Prepared Flyway script: {}", flywayFilename);
            }
        }
    }

    private void runFlywayMigration() {
        ClassicConfiguration configuration = new ClassicConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setLocationsAsStrings(flywayLocations.split(","));
        configuration.setTable(flywayTable);
        configuration.setIgnoreMigrationPatterns("*:WSREP_ON");

        configuration.setRepeatableSqlMigrationPrefix("R__import");
        configuration.setSqlMigrationSeparator("_");

        configuration.setBaselineOnMigrate(true);
        configuration.setValidateOnMigrate(false);
        configuration.setCleanDisabled(true);

        configuration.setValidateMigrationNaming(false);

        Flyway flyway = new Flyway(configuration);
        MigrateResult migrationsApplied = flyway.migrate();
        if (migrationsApplied.success) {
            log.info("Flyway applied {} SDE migrations", migrationsApplied.migrationsExecuted);
            cleanMigrationFiles();
            eveInteractor.sendNotification("eve", "SDE Update Success" ,
                    String.format("Applied %d migrations in %s", migrationsApplied.migrationsExecuted,
                            migrationsApplied.database),
                    "default");
        }
        else {
            log.error("Flyway migration failed");
            eveInteractor.sendNotification("eve", "SDE Update Failed" , String.format("Failed in %s: %s",
                    migrationsApplied.database, migrationsApplied.getException()), "high");
        }
    }

    // Cleanup methods
    private void cleanupTempFiles() {
        try {
            Path tempDirPath = Paths.get(tempDir);
            if (Files.exists(tempDirPath)) {
                Files.walk(tempDirPath)
                        .sorted(Comparator.reverseOrder())
                        .filter(path -> !path.equals(tempDirPath))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                log.warn("Failed to delete temp file: {}", path, e);
                            }
                        });
            }
        } catch (IOException e) {
            log.warn("Error during temp files cleanup", e);
        }
    }

    private void cleanMigrationFiles() {
        String primaryLocation = flywayLocations.split(",")[0].replace("filesystem:", "").trim();
        Path flywayDir = Paths.get(primaryLocation);

        try {
            Files.walk(flywayDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith("R__import_"))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                            log.debug("Deleted migration file: {}", p);
                        } catch (IOException e) {
                            log.warn("Could not delete migration file: {}", p, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Error cleaning migration files", e);
        }
    }

}