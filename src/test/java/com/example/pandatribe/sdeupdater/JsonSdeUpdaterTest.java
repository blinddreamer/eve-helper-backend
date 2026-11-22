package com.example.pandatribe.sdeupdater;

import com.example.pandatribe.sdeupdater.converters.BlueprintsConverter;
import com.example.pandatribe.sdeupdater.converters.MapRegionsConverter;
import com.example.pandatribe.sdeupdater.converters.MapSolarSystemsConverter;
import com.example.pandatribe.sdeupdater.converters.MarketGroupsConverter;
import com.example.pandatribe.sdeupdater.converters.NpcStationsConverter;
import com.example.pandatribe.sdeupdater.converters.PlanetSchematicsConverter;
import com.example.pandatribe.sdeupdater.converters.TypeMaterialsConverter;
import com.example.pandatribe.sdeupdater.converters.TypesConverter;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real updater (download, convert, Flyway, swap) against its own scratch schema,
 * serving a tiny fake SDE release over local HTTP. Needs the same MariaDB as the other tests.
 */
class JsonSdeUpdaterTest {

    private static final String DB = "evesde_sdeupdater_test";
    private static final Map<String, byte[]> RELEASES = new HashMap<>();
    private static HttpServer server;
    private static DriverManagerDataSource dataSource;
    private static JdbcTemplate jdbc;
    private static JdbcTemplate admin; // no default schema, so it can drop/create the test schema

    @TempDir
    Path workDir;

    @BeforeAll
    static void startServerAndDatabase() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            String version = exchange.getRequestURI().getPath().replaceAll("\\D", "");
            byte[] body = RELEASES.get(version);
            exchange.sendResponseHeaders(body == null ? 404 : 200, body == null ? -1 : body.length);
            if (body != null) {
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();

        String host = System.getenv().getOrDefault("DB_HOST", "localhost:3306");
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String pass = System.getenv().getOrDefault("DB_PASS", "test132");
        admin = new JdbcTemplate(new DriverManagerDataSource("jdbc:mariadb://" + host + "/", user, pass));
        dataSource = new DriverManagerDataSource("jdbc:mariadb://" + host + "/" + DB, user, pass);
        jdbc = new JdbcTemplate(dataSource);
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @BeforeEach
    void freshSchema() {
        admin.execute("DROP DATABASE IF EXISTS " + DB);
        admin.execute("CREATE DATABASE " + DB);
    }

    @Test
    void successfulUpdateSwapsInAllTablesAndKeepsStationNames() throws IOException {
        // An existing live table with a name the JSON SDE doesn't ship
        jdbc.execute("CREATE TABLE staStations (stationID BIGINT PRIMARY KEY, stationName VARCHAR(255), "
                + "solarSystemID INT, regionID INT, stationTypeID INT)");
        jdbc.update("INSERT INTO staStations VALUES (60003760, 'Jita IV - Moon 4 - Caldari Navy Assembly Plant', 30000142, NULL, 52678)");
        RELEASES.put("1", release(validFiles()));

        assertThat(updater().update("1")).isTrue();

        assertThat(jdbc.queryForObject("SELECT typeName FROM invTypes WHERE typeID = 587", String.class)).isEqualTo("Rifter");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM industryActivityProducts WHERE typeID = 691", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT security FROM mapSolarSystems WHERE solarSystemID = 30000142", Double.class)).isEqualTo(0.9459);
        assertThat(jdbc.queryForObject("SELECT quantity FROM invTypeMaterials WHERE typeID = 587 AND materialTypeID = 34", Integer.class)).isEqualTo(10);
        assertThat(jdbc.queryForMap("SELECT stationName, regionID FROM staStations WHERE stationID = 60003760"))
                .containsEntry("stationName", "Jita IV - Moon 4 - Caldari Navy Assembly Plant")
                .containsEntry("regionID", 10000002);
        assertThat(leftoverTables()).isZero();
    }

    @Test
    void failedUpdateLeavesLiveTablesUntouched() throws IOException {
        RELEASES.put("1", release(validFiles()));
        assertThat(updater().update("1")).isTrue();

        Map<String, String> broken = validFiles();
        // Duplicate primary key in the staging insert makes the typeMaterials script fail mid-run
        broken.put("typeMaterials.jsonl", "{\"_key\":587,\"materials\":[{\"materialTypeID\":34,\"quantity\":1},{\"materialTypeID\":34,\"quantity\":2}]}\n");
        broken.put("types.jsonl", "{\"_key\":587,\"groupID\":25,\"name\":{\"en\":\"Renamed Rifter\"},\"portionSize\":1,\"published\":true,\"volume\":27289.0}\n");
        RELEASES.put("2", release(broken));

        assertThat(updater().update("2")).isFalse();

        assertThat(jdbc.queryForObject("SELECT typeName FROM invTypes WHERE typeID = 587", String.class)).isEqualTo("Rifter");
        assertThat(jdbc.queryForObject("SELECT quantity FROM invTypeMaterials WHERE typeID = 587", Integer.class)).isEqualTo(10);
        assertThat(leftoverTables()).isZero();
    }

    private int leftoverTables() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? "
                + "AND (table_name LIKE '%\\_new' OR table_name LIKE '%\\_old')", Integer.class, DB);
    }

    private JsonSdeUpdater updater() {
        JsonSdeUpdater updater = new JsonSdeUpdater();
        ReflectionTestUtils.setField(updater, "sdeDownloadUrlTemplate",
                "http://localhost:" + server.getAddress().getPort() + "/sde-{version}.zip");
        ReflectionTestUtils.setField(updater, "tempDir", workDir.resolve("download").toString());
        ReflectionTestUtils.setField(updater, "flywayLocations", "filesystem:" + workDir.resolve("sql"));
        ReflectionTestUtils.setField(updater, "flywayTable", "sde_schema_history");
        ReflectionTestUtils.setField(updater, "dataSource", dataSource);
        ReflectionTestUtils.setField(updater, "blueprintsConverter", new BlueprintsConverter());
        ReflectionTestUtils.setField(updater, "typesConverter", new TypesConverter());
        ReflectionTestUtils.setField(updater, "typeMaterialsConverter", new TypeMaterialsConverter());
        ReflectionTestUtils.setField(updater, "marketGroupsConverter", new MarketGroupsConverter());
        ReflectionTestUtils.setField(updater, "mapRegionsConverter", new MapRegionsConverter());
        ReflectionTestUtils.setField(updater, "mapSolarSystemsConverter", new MapSolarSystemsConverter());
        ReflectionTestUtils.setField(updater, "npcStationsConverter", new NpcStationsConverter());
        ReflectionTestUtils.setField(updater, "planetSchematicsConverter", new PlanetSchematicsConverter());
        updater.init();
        return updater;
    }

    /** One realistic record per file, in the shape of SDE build 3698585. */
    private static Map<String, String> validFiles() {
        Map<String, String> files = new HashMap<>();
        files.put("blueprints.jsonl", "{\"_key\":691,\"activities\":{\"manufacturing\":{\"materials\":[{\"quantity\":86,\"typeID\":34}],\"products\":[{\"quantity\":1,\"typeID\":587}],\"time\":600}},\"maxProductionLimit\":300}\n");
        files.put("types.jsonl", "{\"_key\":587,\"groupID\":25,\"name\":{\"en\":\"Rifter\"},\"portionSize\":1,\"published\":true,\"volume\":27289.0,\"marketGroupID\":64}\n");
        files.put("typeMaterials.jsonl", "{\"_key\":587,\"materials\":[{\"materialTypeID\":34,\"quantity\":10}]}\n{\"_key\":1,\"randomizedMaterials\":[]}\n");
        files.put("marketGroups.jsonl", "{\"_key\":64,\"hasTypes\":true,\"name\":{\"en\":\"Frigates\"}}\n");
        files.put("mapRegions.jsonl", "{\"_key\":10000002,\"name\":{\"en\":\"The Forge\"},\"nebulaID\":1,\"position\":{\"x\":1,\"y\":2,\"z\":3}}\n");
        files.put("mapSolarSystems.jsonl", "{\"_key\":30000142,\"constellationID\":20000020,\"name\":{\"en\":\"Jita\"},\"regionID\":10000002,\"securityStatus\":0.9459,\"securityClass\":\"B\"}\n");
        files.put("npcStations.jsonl", "{\"_key\":60003760,\"solarSystemID\":30000142,\"typeID\":52678}\n");
        files.put("planetSchematics.jsonl", "{\"_key\":65,\"cycleTime\":3600,\"name\":{\"en\":\"Superconductors\"},\"types\":[{\"_key\":2389,\"isInput\":true,\"quantity\":40}]}\n");
        return files;
    }

    private static byte[] release(Map<String, String> files) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, String> file : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
