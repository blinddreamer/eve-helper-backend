package com.example.pandatribe.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Dumps request/response bodies as JSON at DEBUG level (off in production by default).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonLogger {
    private final ObjectMapper objectMapper;

    public void println(Object object) {
        if (!log.isDebugEnabled()) {
            return; // skip serializing large calculator payloads when nobody will read them
        }
        try {
            log.debug(objectMapper.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            log.debug("Could not serialize {}: {}", object.getClass().getSimpleName(), e.getMessage());
        }
    }
}
