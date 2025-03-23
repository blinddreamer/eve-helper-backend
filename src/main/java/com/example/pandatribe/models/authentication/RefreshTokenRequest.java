package com.example.pandatribe.models.authentication;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefreshTokenRequest {
    private String grant_type;
    private String client_id;
    private String client_secret;
    private String refresh_token;

}
