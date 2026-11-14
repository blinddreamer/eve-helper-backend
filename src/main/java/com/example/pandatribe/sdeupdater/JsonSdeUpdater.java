package com.example.pandatribe.sdeupdater;

import com.example.pandatribe.configs.CacheConfig;
import com.example.pandatribe.feign.contracts.EveInteractor;
import com.example.pandatribe.sdeupdater.converters.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Updates the EVE SDE (Static Data Export) from the official JSONL release.
 *
 * Downloads the release, converts each JSONL file to a SQL script that fills "{table}_new"
 * staging tables, runs the scripts with Flyway, and only when every script succeeded swaps all
 * staging tables in with a single atomic RENAME TABLE. On any failure the staging tables are
 * dropped and the live tables are left untouched.
 */
@Slf4j
@Service
public class JsonSdeUpdater {

    private static final String STAGING = "_new";
    private static final String RETIRED = "_old";

    @Value("${eve.sde.json.download.url.template:https://developers.eveonline.com/static-data/tranquility/eve-online-static-data-{version}-jsonl.zip}")
    private String sdeDownloadUrlTemplate;

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

    @Autowired(required = false)
    private CacheConfig cacheConfig;

    @Autowired
    private BlueprintsConverter blueprintsConverter;
    @Autowired
    private TypesConverter typesConverter;
    @Autowired
    private TypeMaterialsConverter typeMaterialsConverter;
    @Autowired
    private MarketGroupsConverter marketGroupsConverter;
    @Autowired
    private MapRegionsConverter mapRegionsConverter;
    @Autowired
    private MapSolarSystemsConverter mapSolarSystemsConverter;
    @Autowired
    private NpcStationsConverter npcStationsConverter;
    @Autowired
    private PlanetSchematicsConverter planetSchematicsConverter;

    // JSONL file name -> converter
    private final Map<String, SdeJsonToSqlConverter> converterMap = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        createDirectories(Paths.get(tempDir));
        createDirectories(flywayDir());

        converterMap.put("blueprints.jsonl", blueprintsConverter);
        converterMap.put("types.jsonl", typesConverter);
        converterMap.put("typeMaterials.jsonl", typeMaterialsConverter);
        converterMap.put("marketGroups.jsonl", marketGroupsConverter);
        converterMap.put("mapRegions.jsonl", mapRegionsConverter);
        converterMap.put("mapSolarSystems.jsonl", mapSolarSystemsConverter);
        converterMap.put("npcStations.jsonl", npcStationsConverter);
        converterMap.put("planetSchematics.jsonl", planetSchematicsConverter);
    }

    /**
     * Downloads and applies the given SDE build.
     *
     * @return true only if the new data is live; false if anything failed (live data untouched)
     */
    public boolean update(String version) {
        log.info("Starting SDE update to build {}", version);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try {
            dropTables(jdbc, STAGING);
            dropTables(jdbc, RETIRED);

            downloadSdeZip(version);
            extractSdeZip();
            convertJsonToSql(version);
            runFlywayMigration();
            swapTables(jdbc);
            clearStaticDataCaches();

            log.info("SDE update to build {} completed successfully", version);
            sendNotificationSafely("eve", "SDE Update Success", "Now on SDE build " + version, "default");
            return true;
        } catch (Exception e) {
            log.error("SDE update to build {} failed, live data left untouched", version, e);
            try {
                dropTables(jdbc, STAGING);
            } catch (Exception cleanup) {
                log.warn("Could not drop SDE staging tables", cleanup);
            }
            sendNotificationSafely("eve", "SDE Update Failed",
                String.format("Build %s: %s", version, e.getMessage()), "high");
            return false;
        } finally {
            deleteFiles(flywayDir(), p -> p.getFileName().toString().startsWith("R__import_"));
            deleteFiles(Paths.get(tempDir), p -> true);
        }
    }

    private List<String> liveTables() {
        return converterMap.values().stream()
            .flatMap(c -> c.tables().stream())
            .toList();
    }

    /**
     * Replaces every live table with its staging copy in one atomic RENAME TABLE.
     */
    private void swapTables(JdbcTemplate jdbc) {
        List<String> tables = liveTables();
        for (String table : tables) {
            // RENAME needs every source table to exist (fresh databases may lack some)
            jdbc.execute("CREATE TABLE IF NOT EXISTS `" + table + "` LIKE `" + table + STAGING + "`");
        }
        String renames = tables.stream()
            .map(t -> String.format("`%1$s` TO `%1$s%2$s`, `%1$s%3$s` TO `%1$s`", t, RETIRED, STAGING))
            .collect(Collectors.joining(", "));
        jdbc.execute("RENAME TABLE " + renames);
        log.info("Swapped in {} SDE tables", tables.size());
        dropTables(jdbc, RETIRED);
    }

    private void dropTables(JdbcTemplate jdbc, String suffix) {
        String names = liveTables().stream()
            .map(t -> "`" + t + suffix + "`")
            .collect(Collectors.joining(", "));
        jdbc.execute("DROP TABLE IF EXISTS " + names);
    }

    private void downloadSdeZip(String version) throws IOException {
        String downloadUrl = sdeDownloadUrlTemplate.replace("{version}", version);
        Path targetFile = Paths.get(tempDir, "sde.zip");
        Files.createDirectories(targetFile.getParent());

        log.info("Downloading SDE ZIP version {} from: {}", version, downloadUrl);

        HttpURLConnection connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
        connection.setConnectTimeout(60000);
        connection.setReadTimeout(300000); // 5 minutes for large file
        connection.setRequestProperty("User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        connection.setRequestProperty("Referer", "https://developers.eveonline.com/resource/resources");
        connection.setRequestProperty("Accept", "application/zip, application/octet-stream, */*");

        try (InputStream in = connection.getInputStream()) {
            long bytes = Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("Downloaded SDE ZIP: {} MB", bytes / (1024 * 1024));
        } finally {
            connection.disconnect();
        }
    }

    private void extractSdeZip() throws IOException {
        Path zipFile = Paths.get(tempDir, "sde.zip");
        Path extractDir = Paths.get(tempDir, "extracted");
        Files.createDirectories(extractDir);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            int extractedCount = 0;
            while ((entry = zis.getNextEntry()) != null) {
                String baseName = Paths.get(entry.getName()).getFileName().toString();
                if (converterMap.containsKey(baseName)) {
                    Files.copy(zis, extractDir.resolve(baseName), StandardCopyOption.REPLACE_EXISTING);
                    extractedCount++;
                    log.info("Extracted: {}", baseName);
                }
                zis.closeEntry();
            }
            log.info("Extracted {} JSON files", extractedCount);
        }

        Files.deleteIfExists(zipFile);
    }

    private void convertJsonToSql(String version) throws IOException {
        Path extractDir = Paths.get(tempDir, "extracted");
        Path flywayDir = flywayDir();
        Files.createDirectories(flywayDir);
        deleteFiles(flywayDir, p -> p.getFileName().toString().startsWith("R__import_"));

        // Unique per run so Flyway treats every script as changed and re-runs all of them
        String header = "SDE build " + version + ", run " + UUID.randomUUID();

        for (Map.Entry<String, SdeJsonToSqlConverter> entry : converterMap.entrySet()) {
            Path inputPath = extractDir.resolve(entry.getKey());
            if (!Files.exists(inputPath)) {
                // Every converter's staging tables are swapped in, so a missing file is fatal
                throw new IOException("SDE release is missing " + entry.getKey());
            }
            String sqlFileName = String.format("R__import_%s.sql", entry.getKey().replace(".jsonl", ""));
            entry.getValue().convertToSql(inputPath, flywayDir.resolve(sqlFileName), header);
        }
        log.info("Converted {} JSON files to SQL", converterMap.size());
    }

    private void runFlywayMigration() {
        log.info("Loading SDE staging tables via Flyway");

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

        MigrateResult result = new Flyway(configuration).migrate();
        if (!result.success) {
            throw new IllegalStateException("Flyway SDE migration failed: " + result.getException());
        }
        log.info("Flyway applied {} SDE scripts", result.migrationsExecuted);
    }

    private Path flywayDir() {
        return Paths.get(flywayLocations.split(",")[0].replace("filesystem:", "").trim());
    }

    private void deleteFiles(Path dir, java.util.function.Predicate<Path> filter) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder())
                .filter(p -> !p.equals(dir) && filter.test(p))
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        log.warn("Could not delete {}", p, e);
                    }
                });
        } catch (IOException e) {
            log.warn("Error cleaning {}", dir, e);
        }
    }

    private void createDirectories(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create directory: " + dir, e);
        }
    }

    private void clearStaticDataCaches() {
        if (cacheConfig == null) {
            return;
        }
        try {
            cacheConfig.evictAllStaticDataCaches();
            cacheConfig.evictCacheCalculator();
        } catch (Exception e) {
            log.warn("Failed to clear static data caches after SDE update", e);
        }
    }

    /**
     * Notification failures (e.g. 403 from ntfy) must never break the update itself.
     */
    private void sendNotificationSafely(String topic, String title, String message, String priority) {
        if (eveInteractor == null) {
            return;
        }
        try {
            eveInteractor.sendNotification(topic, title, message, priority);
        } catch (Exception e) {
            log.warn("Failed to send notification '{}': {} - {}", title, e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
