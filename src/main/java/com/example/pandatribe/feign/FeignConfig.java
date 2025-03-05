package com.example.pandatribe.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Logger;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class FeignConfig {

    private final ObjectMapper objectMapper;

    public <T> T getRestClient(Class<T> cls, String url) {
        return Feign.builder()
                .encoder(new JacksonEncoder(objectMapper)) // Use Form encoding for token request
                .decoder(new JacksonDecoder(objectMapper))
                .logger(new EveHelperLogger())
                .logLevel(Logger.Level.BASIC)
                .target(cls, url);
    }
}
