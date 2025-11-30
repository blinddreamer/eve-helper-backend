package com.example.pandatribe.exceptions;

public class RequestNotFoundException extends RuntimeException {
    private final String requestId;

    public RequestNotFoundException(String requestId) {
        super(String.format("Request with ID '%s' not found", requestId));
        this.requestId = requestId;
    }

    public RequestNotFoundException(String requestId, Throwable cause) {
        super(String.format("Request with ID '%s' not found", requestId), cause);
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }
}