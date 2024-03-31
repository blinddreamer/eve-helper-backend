package com.example.pandatribe.services;

import com.example.pandatribe.services.contracts.AuthService;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AuthServiceImpl implements AuthService {

    public String getAuthUrl(){
    String clientId = "a9a1b3c2441a49698a0e4a332746b299";
    String redirectUri = "http://localhost/gnomcho";

    // Generate a random state parameter for security
    String state = generateRandomState();

    // Specify additional parameters (scope, etc.) as needed
    String scope = "publicData";

    // Build the EVE Online login link with URL encoding
    String loginLink = buildEveOnlineLoginLink(clientId, redirectUri, state, scope);

    // Print the login link
       return loginLink;
}

private  String generateRandomState() {
    byte[] randomBytes = new byte[16];
    new SecureRandom().nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
}

private String buildEveOnlineLoginLink(String clientId, String redirectUri, String state, String scope) {

        try {
        // Specify the EVE Online authorization endpoint URL
        String authorizationEndpoint = "https://login.eveonline.com/v2/oauth/authorize";

        // Construct the EVE Online login link with URL encoding
        return String.format("%s?response_type=code&client_id=%s&redirect_uri=%s&state=%s&scope=%s",
                authorizationEndpoint,
                URLEncoder.encode(clientId, StandardCharsets.UTF_8.toString()),
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString()),
                URLEncoder.encode(state, StandardCharsets.UTF_8.toString()),
                URLEncoder.encode(scope, StandardCharsets.UTF_8.toString()));
    } catch (Exception e) {
        // Handle the exception appropriately
        e.printStackTrace();
        return null;
    }
}
}
