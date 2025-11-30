package com.example.pandatribe.exceptions;

public class InvalidRequestException extends RuntimeException {
    private final String fieldName;

    public InvalidRequestException(String message) {
        super(message);
        this.fieldName = null;
    }

    public InvalidRequestException(String fieldName, String message) {
        super(String.format("Invalid field '%s': %s", fieldName, message));
        this.fieldName = fieldName;
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
        this.fieldName = null;
    }

    public String getFieldName() {
        return fieldName;
    }
}