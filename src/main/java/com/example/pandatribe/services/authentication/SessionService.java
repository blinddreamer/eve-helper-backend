package com.example.pandatribe.services.authentication;

import com.example.pandatribe.models.dbmodels.auth.UserSession;
import com.example.pandatribe.repositories.interfaces.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Login sessions: the cookie carries a random token, the database stores its SHA-256 hash.
 * Sessions expire after {@link #SESSION_TTL} unless extended, and logout deletes them.
 */
@Service
@RequiredArgsConstructor
public class SessionService {

    public static final String SESSION_COOKIE = "session";
    public static final Duration SESSION_TTL = Duration.ofDays(7);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserSessionRepository sessionRepository;

    /** URL-safe random token with 256 bits of entropy. */
    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String create(String accountId) {
        String token = randomToken();
        sessionRepository.save(new UserSession(hash(token), accountId, Instant.now().plus(SESSION_TTL)));
        return token;
    }

    public Optional<String> resolveAccountId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return sessionRepository.findByTokenHashAndExpiresAtAfter(hash(token), Instant.now())
                .map(UserSession::getAccountId);
    }

    /** @return false if the session is unknown or already expired */
    public boolean extend(String token) {
        return token != null && sessionRepository.findByTokenHashAndExpiresAtAfter(hash(token), Instant.now())
                .map(session -> {
                    session.setExpiresAt(Instant.now().plus(SESSION_TTL));
                    sessionRepository.save(session);
                    return true;
                })
                .orElse(false);
    }

    public void delete(String token) {
        if (token != null) {
            sessionRepository.deleteById(hash(token));
        }
    }

    public int deleteExpired() {
        return sessionRepository.deleteExpired(Instant.now());
    }

    public static ResponseCookie sessionCookie(String token) {
        return cookie(SESSION_COOKIE, token, "/", SESSION_TTL);
    }

    public static ResponseCookie clearedSessionCookie() {
        return cookie(SESSION_COOKIE, "", "/", Duration.ZERO);
    }

    public static ResponseCookie cookie(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
