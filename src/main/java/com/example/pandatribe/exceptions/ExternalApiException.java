package com.example.pandatribe.exceptions;

public class ExternalApiException extends RuntimeException {
    private final String apiName;
    private final Integer statusCode;

    public ExternalApiException(String apiName, String message) {
        super(String.format("External API '%s' error: %s", apiName, message));
        this.apiName = apiName;
        this.statusCode = null;
    }

    public ExternalApiException(String apiName, Integer statusCode, String message) {
        super(String.format("External API '%s' returned status %d: %s", apiName, statusCode, message));
        this.apiName = apiName;
        this.statusCode = statusCode;
    }

    public ExternalApiException(String apiName, String message, Throwable cause) {
        super(String.format("External API '%s' error: %s", apiName, message), cause);
        this.apiName = apiName;
        this.statusCode = null;
    }

    public String getApiName() {
        return apiName;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}