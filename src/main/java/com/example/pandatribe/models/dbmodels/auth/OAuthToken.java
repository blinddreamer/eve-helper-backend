package com.example.pandatribe.models.dbmodels.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "oauth_tokens")
@Builder
@With
@Data
@NoArgsConstructor
@AllArgsConstructor

public class OAuthToken {

    @Column(name = "account_id")
    private String accountId;
    @Id
    @Column(name = "character_id")
    private Integer characterId;

    @Column(name = "access_token")
    private String accessToken;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "isPrimary")
    private Boolean isPrimary;
}
