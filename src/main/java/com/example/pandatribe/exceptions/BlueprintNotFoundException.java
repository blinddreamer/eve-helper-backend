package com.example.pandatribe.exceptions;

public class BlueprintNotFoundException extends RuntimeException {
    private final String blueprintName;

    public BlueprintNotFoundException(String blueprintName) {
        super(String.format("Blueprint '%s' not found", blueprintName));
        this.blueprintName = blueprintName;
    }

    public BlueprintNotFoundException(String blueprintName, Throwable cause) {
        super(String.format("Blueprint '%s' not found", blueprintName), cause);
        this.blueprintName = blueprintName;
    }

    public String getBlueprintName() {
        return blueprintName;
    }
}