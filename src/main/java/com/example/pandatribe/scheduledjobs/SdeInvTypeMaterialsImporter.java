package com.example.pandatribe.scheduledjobs;

import com.example.pandatribe.repositories.interfaces.InvTypeMaterialRepository;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Component
public class SdeInvTypeMaterialsImporter implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdeInvTypeMaterialsImporter.class);
    private static final String FUZZWORK_URL = "https://www.fuzzwork.co.uk/dump/latest/invTypeMaterials.sql.bz2";

    private final InvTypeMaterialRepository invTypeMaterialRepository;
    private final JdbcTemplate jdbcTemplate;

    public SdeInvTypeMaterialsImporter(InvTypeMaterialRepository invTypeMaterialRepository,
                                       JdbcTemplate jdbcTemplate) {
        this.invTypeMaterialRepository = invTypeMaterialRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        long count = invTypeMaterialRepository.count();
        if (count > 0) {
            LOGGER.info("invTypeMaterials already has {} rows — skipping import", count);
            return;
        }

        LOGGER.info("invTypeMaterials is empty — downloading SDE data from Fuzzwork...");

        try (InputStream raw = URI.create(FUZZWORK_URL).toURL().openStream();
             BZip2CompressorInputStream bz2 = new BZip2CompressorInputStream(raw);
             BufferedReader reader = new BufferedReader(new InputStreamReader(bz2, StandardCharsets.UTF_8))) {

            int batches = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith("INSERT INTO")) continue;

                // Strip trailing semicolon if present — jdbcTemplate doesn't want it
                if (line.endsWith(";")) line = line.substring(0, line.length() - 1);

                jdbcTemplate.update(line);
                batches++;

                if (batches % 500 == 0) {
                    LOGGER.info("invTypeMaterials import progress: {} batches inserted", batches);
                }
            }

            long total = invTypeMaterialRepository.count();
            LOGGER.info("invTypeMaterials import complete — {} INSERT batches, {} total rows", batches, total);

        } catch (Exception e) {
            LOGGER.error("Failed to import invTypeMaterials from Fuzzwork — reprocess will not work until resolved: {}", e.getMessage());
        }
    }
}
