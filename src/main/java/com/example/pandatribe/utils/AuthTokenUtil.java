package com.example.pandatribe.utils;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility class for authentication token operations.
 * Handles Basic Auth token generation and Base64 encoding/decoding.
 */
@Component
public class AuthTokenUtil {

    /**
     * Generates a Basic Authentication token from client credentials.
     *
     * @param clientId The client ID
     * @param clientSecret The client secret
     * @return Base64 encoded "clientId:clientSecret" string
     */
    public String generateBasicAuthToken(String clientId, String clientSecret) {
        String authValue = clientId + ":" + clientSecret;
        return Base64.getEncoder().encodeToString(authValue.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encodes a string to Base64.
     * Note: This is NOT encryption, just encoding.
     *
     * @param data The string to encode
     * @return Base64 encoded string
     */
    public String encodeBase64(String data) {
        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes a Base64 string.
     * Note: This is NOT decryption, just decoding.
     *
     * @param encodedData The Base64 encoded string
     * @return Decoded string
     */
    public String decodeBase64(String encodedData) {
        return new String(Base64.getDecoder().decode(encodedData), StandardCharsets.UTF_8);
    }
}
