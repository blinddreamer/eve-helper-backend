package com.example.pandatribe.models.authentication;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenRequest {
    private String grantType;
    private String code;
}
