package com.example.pandatribe.sdeupdater;

import com.example.pandatribe.feign.contracts.EveInteractor;
import com.example.pandatribe.sdeupdater.converters.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service to update the EVE SDE (Static Data Export) from the official JSON format.
 * Downloads the latest SDE ZIP file, extracts JSON files, converts them to SQL,
 * and applies the migrations using Flyway.
 */
@Slf4j
@Service
public class JsonSdeUpdater {

    @Value("${eve.sde.json.download.url:https://eve-static-data-export.s3-eu-west-1.amazonaws.com/tranquility/sde.zip}")
    private String sdeDownloadUrl;

    @Value("${eve.sde.temp.dir:temp_sde_downloads}")
    private String tempDir;

    @Value("${eve.sde.flyway.locations:filesystem:db/sde_migrations}")
    private String flywayLocations;

    @Value("${eve.sde.flyway.table:sde_schema_history}")
    private String flywayTable;

    @Autowired(required = false)
    private EveInteractor eveInteractor;

    @Autowired
    private DataSource dataSource;

    // Converter beans
    @Autowired
    private BlueprintsConverter blueprintsConverter;
    @Autowired
    private TypesConverter typesConverter;
    @Autowired
    private MapRegionsConverter mapRegionsConverter;
    @Autowired
    private MapSolarSystemsConverter mapSolarSystemsConverter;
    @Autowired
    private NpcStationsConverter npcStationsConverter;
    @Autowired
    private PlanetSchematicsConverter planetSchematicsConverter;

    // Map of JSON files to converters
    private final Map<String, SdeJsonToSqlConverter> converterMap = new HashMap<>();

    @PostConstruct
    public void init() {
        ensureDirectoryExists();
        ensureFlywayDirectoryExists();

        // Map JSON files to their converters
        converterMap.put("blueprints.jsonl", blueprintsConverter);
        converterMap.put("types.jsonl", typesConverter);
        converterMap.put("mapRegions.jsonl", mapRegionsConverter);
        converterMap.put("mapSolarSystems.jsonl", mapSolarSystemsConverter);
        converterMap.put("npcStations.jsonl", npcStationsConverter);
        converterMap.put("planetSchematics.jsonl", planetSchematicsConverter);
    }

    /**
     * Main method to check for updates and process them
     */
    public void checkAndUpdate() {
        try {
            if (!updateTriggerExists()) {
                log.debug("No update trigger found, skipping SDE update");
                return;
            }

            log.info("Starting SDE update process (JSON format)");

            downloadSdeZip();
            extractSdeZip();
            convertJsonToSql();
            runFlywayMigration();
            clearUpdateTrigger();
            cleanupTempFiles();

            log.info("SDE update completed successfully");
        } catch (Exception e) {
            log.error("Error during SDE update process", e);
            if (eveInteractor != null) {
                eveInteractor.sendNotification("eve", "SDE Update Failed",
                    String.format("Error: %s", e.getMessage()), "high");
            }
        }
    }

    /**
     * Downloads the SDE ZIP file from the official source
     */
    private void downloadSdeZip() throws IOException {
        Path targetFile = Paths.get(tempDir, "sde.zip");
        Files.createDirectories(targetFile.getParent());

        log.info("Downloading SDE ZIP from: {}", sdeDownloadUrl);

        URL url = new URL(sdeDownloadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(60000);
        connection.setReadTimeout(300000); // 5 minutes for large file

        try (InputStream in = connection.getInputStream();
             OutputStream out = Files.newOutputStream(targetFile,
                 StandardOpenOption.CREATE,
                 StandardOpenOption.TRUNCATE_EXISTING,
                 StandardOpenOption.WRITE)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;

                if (totalBytes % (10 * 1024 * 1024) == 0) { // Log every 10MB
                    log.debug("Downloaded {} MB", totalBytes / (1024 * 1024));
                }
            }

            log.info("Downloaded SDE ZIP: {} MB", totalBytes / (1024 * 1024));
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Extracts required JSON files from the SDE ZIP
     */
    private void extractSdeZip() throws IOException {
        Path zipFile = Paths.get(tempDir, "sde.zip");
        Path extractDir = Paths.get(tempDir, "extracted");
        Files.createDirectories(extractDir);

        log.info("Extracting SDE ZIP file");

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            int extractedCount = 0;

            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName();

                // Only extract files we need
                if (!fileName.endsWith(".jsonl")) {
                    continue;
                }

                // Get just the file name without path
                String baseName = Paths.get(fileName).getFileName().toString();

                if (converterMap.containsKey(baseName)) {
                    Path targetFile = extractDir.resolve(baseName);

                    try (OutputStream out = Files.newOutputStream(targetFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }

                    extractedCount++;
                    log.info("Extracted: {}", baseName);
                }

                zis.closeEntry();
            }

            log.info("Extracted {} JSON files", extractedCount);
        }

        // Delete the ZIP file to save space
        Files.deleteIfExists(zipFile);
    }

    /**
     * Converts extracted JSON files to SQL migration scripts
     */
    private void convertJsonToSql() throws IOException {
        Path extractDir = Paths.get(tempDir, "extracted");
        Path flywayDir = Paths.get(flywayLocations.split(",")[0].replace("filesystem:", "").trim());
        Files.createDirectories(flywayDir);

        log.info("Converting JSON files to SQL");

        // Clear old migration files
        clearOldMigrations(flywayDir);

        int fileCount = 0;
        for (Map.Entry<String, SdeJsonToSqlConverter> entry : converterMap.entrySet()) {
            String jsonFile = entry.getKey();
            SdeJsonToSqlConverter converter = entry.getValue();

            Path inputPath = extractDir.resolve(jsonFile);
            if (!Files.exists(inputPath)) {
                log.warn("Expected JSON file not found: {}", jsonFile);
                continue;
            }

            // Generate SQL file name using Flyway repeatable migration naming
            String sqlFileName = String.format("R__import_%s.sql",
                jsonFile.replace(".jsonl", ""));
            Path outputPath = flywayDir.resolve(sqlFileName);

            converter.convertToSql(inputPath, outputPath);
            fileCount++;
        }

        log.info("Converted {} JSON files to SQL", fileCount);
    }

    /**
     * Runs Flyway migrations to apply SQL scripts to the database
     */
    private void runFlywayMigration() {
        log.info("Running Flyway migrations");

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
        MigrateResult result = flyway.migrate();

        if (result.success) {
            log.info("Flyway applied {} SDE migrations successfully", result.migrationsExecuted);
            if (eveInteractor != null) {
                eveInteractor.sendNotification("eve", "SDE Update Success",
                    String.format("Applied %d migrations", result.migrationsExecuted),
                    "default");
            }

            // Clean up migration files after successful application
            cleanMigrationFiles();
        } else {
            log.error("Flyway migration failed");
            if (eveInteractor != null) {
                eveInteractor.sendNotification("eve", "SDE Update Failed",
                    String.format("Migration failed: %s", result.getException()),
                    "high");
            }
        }
    }

    /**
     * Clears old repeatable migration files
     */
    private void clearOldMigrations(Path flywayDir) throws IOException {
        if (!Files.exists(flywayDir)) {
            return;
        }

        Files.walk(flywayDir)
            .filter(Files::isRegularFile)
            .filter(p -> p.getFileName().toString().startsWith("R__import_"))
            .forEach(p -> {
                try {
                    Files.delete(p);
                    log.debug("Deleted old migration: {}", p.getFileName());
                } catch (IOException e) {
                    log.warn("Could not delete old migration file: {}", p, e);
                }
            });
    }

    /**
     * Cleans up migration files after successful application
     */
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
                        log.debug("Cleaned up migration file: {}", p.getFileName());
                    } catch (IOException e) {
                        log.warn("Could not delete migration file: {}", p, e);
                    }
                });
        } catch (IOException e) {
            log.warn("Error cleaning migration files", e);
        }
    }

    /**
     * Cleans up temporary files and directories
     */
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
            log.debug("Cleaned up temporary files");
        } catch (IOException e) {
            log.warn("Error during temp files cleanup", e);
        }
    }

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

    private void clearUpdateTrigger() throws IOException {
        Files.deleteIfExists(Paths.get("data/sde_update_trigger.txt"));
        log.debug("Cleared update trigger");
    }
}
