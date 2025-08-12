package com.example.pandatribe.feign.contracts;


import feign.HeaderMap;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.Map;

@Headers({"Accept: application/json",
        "Content-type: application/json"})
public interface ExternalApi {

    @RequestLine("POST /{topic}")
    @Headers("Content-Type: text/plain")
    void sendNotification(@Param("topic") String topic,
                          @HeaderMap Map<String, String> headers,
                          String message);
}
