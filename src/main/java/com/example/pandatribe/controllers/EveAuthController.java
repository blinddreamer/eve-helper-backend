package com.example.pandatribe.controllers;

import com.example.pandatribe.models.dbmodels.auth.OAuthToken;
import com.example.pandatribe.models.results.CharResult;
import com.example.pandatribe.services.authentication.OAuthTokenService;
import com.example.pandatribe.services.character.CharacterServiceImpl;
import com.example.pandatribe.utils.Helper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class EveAuthController {
    private final OAuthTokenService tokenService;
    private final CharacterServiceImpl characterService;
    private final Helper helper;

    @GetMapping("/callback")
    public void handleCallback(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
        log.info("Received code {}", code);
        // Use the authorization code to get access and refresh tokens
        OAuthToken savedToken =  tokenService.exchangeCodeForTokens(code);

        String cookieValue = helper.compressUUID(UUID.fromString(savedToken.getAccountId()));
        Cookie sessionCookie = new Cookie("sessionUUID", cookieValue);
        sessionCookie.setHttpOnly(true);
        sessionCookie.setSecure(true);
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(60 * 60 * 7 * 24); // 7 days expiration
        response.addCookie(sessionCookie);
        response.setContentType("text/html");
        response.getWriter().write("<script>"
                + "window.opener.postMessage('http://localhost:3000');"
                + "window.close();</script>");
    }

    @GetMapping("/auth/me")
    public ResponseEntity<?> getUserInfo(@CookieValue(value = "sessionUUID", required = false) String sessionUUID) {
        if (sessionUUID == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No active session");
        }
        CharResult character = characterService.getCharacter(helper.decompressUUID(sessionUUID));
        if (character == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Session expired or invalid");
        }

        return ResponseEntity.ok(Map.of("character", character));
    }

    @PostMapping("/auth/extend-session")
    public ResponseEntity<?> extendSession(@CookieValue(value = "sessionUUID", required = false) String sessionUUID, HttpServletResponse response) {
        if (sessionUUID == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Session expired, please log in again");
        }

        // Check if session is valid (exists in DB)
        String id = helper.decompressUUID(sessionUUID).toString();
        OAuthToken token = tokenService.checkTokenExist(id);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid session, please log in again");
        }

        // Extend session duration (e.g., 7 days)
        Cookie newSessionCookie = new Cookie("sessionUUID", sessionUUID);
        newSessionCookie.setHttpOnly(true);
        newSessionCookie.setSecure(true);
        newSessionCookie.setPath("/");
        newSessionCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days

        response.addCookie(newSessionCookie);

        return ResponseEntity.ok("Session extended");
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("sessionUUID", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Expire immediately

        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/auth/wallet")
    public ResponseEntity<?> getWalletBalance(@CookieValue(value = "sessionUUID", required = false) String sessionUUID) {
        if (sessionUUID == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Session expired, please log in again");
        }
        String id = helper.decompressUUID(sessionUUID).toString();
        OAuthToken token = tokenService.checkTokenExist(id);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid session, please log in again");
        }
        BigDecimal walletBalance = characterService.getWalletBalances(id);
        return ResponseEntity.ok(walletBalance);
    }
}
