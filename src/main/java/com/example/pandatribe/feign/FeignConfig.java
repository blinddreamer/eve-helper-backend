package com.example.pandatribe.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Logger;
import feign.RequestInterceptor;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FeignConfig {

    private final ObjectMapper objectMapper;

    public <T> T getRestClient(Class<T> cls, String url) {
        return Feign.builder()
                .encoder(new JacksonEncoder(objectMapper))
                .decoder(new JacksonDecoder(objectMapper))
                .logger(new EveHelperLogger())
                .logLevel(Logger.Level.BASIC)
                .target(cls, url);
    }

    public <T> T getRestClientWithAuthentication(Class<T> cls, String url, String token, String type) {
        return Feign.builder()
                .encoder(new JacksonEncoder(objectMapper))
                .decoder(new JacksonDecoder(objectMapper))
                .logger(new EveHelperLogger())
                .logLevel(Logger.Level.BASIC)
                .requestInterceptor(requestInterceptor(token, type))
                .target(cls, url);
    }


    private RequestInterceptor requestInterceptor(String token, String type) {
        return template -> template.header("Authorization", type + token);
    }
}
