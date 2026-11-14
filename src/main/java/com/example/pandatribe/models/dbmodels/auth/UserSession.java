package com.example.pandatribe.models.dbmodels.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "user_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    @Id
    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "account_id")
    private String accountId;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
