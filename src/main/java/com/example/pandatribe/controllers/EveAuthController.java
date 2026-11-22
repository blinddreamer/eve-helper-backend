package com.example.pandatribe.controllers;

import com.example.pandatribe.models.dbmodels.auth.OAuthToken;
import com.example.pandatribe.models.results.CharResult;
import com.example.pandatribe.services.authentication.OAuthTokenService;
import com.example.pandatribe.services.authentication.SessionService;
import com.example.pandatribe.services.character.CharacterServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EveAuthController {
    private static final String STATE_COOKIE = "oauth_state";
    private static final String CALLBACK_PATH = "/api/v1/callback";
    private static final String AUTHORIZE_URL = "https://login.eveonline.com/v2/oauth/authorize";
    private static final String SCOPES = "publicData esi-wallet.read_character_wallet.v1";

    private final OAuthTokenService tokenService;
    private final CharacterServiceImpl characterService;
    private final SessionService sessionService;

    @Value("${EVE_CLIENT_ID}")
    private String clientId;

    @Value("${frontend.base.url}")
    private String frontendBaseUrl;

    /**
     * Starts EVE SSO. The random state goes in a short-lived cookie scoped to the callback,
     * and the callback rejects any response whose state doesn't match it (login CSRF).
     */
    @GetMapping("/login")
    public ResponseEntity<Void> login(HttpServletRequest request) {
        String state = SessionService.randomToken();
        URI authorize = UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", callbackUrl(request))
                .queryParam("client_id", clientId)
                .queryParam("scope", SCOPES)
                .queryParam("state", state)
                .encode()
                .build()
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(authorize)
                .header(HttpHeaders.SET_COOKIE,
                        SessionService.cookie(STATE_COOKIE, state, CALLBACK_PATH, Duration.ofMinutes(10)).toString())
                .build();
    }

    @GetMapping("/callback") // = CALLBACK_PATH under the class-level /api/v1
    public ResponseEntity<String> handleCallback(@RequestParam("code") String code,
                                                 @RequestParam(value = "state", required = false) String state,
                                                 @CookieValue(value = STATE_COOKIE, required = false) String expectedState) {
        String clearState = SessionService.cookie(STATE_COOKIE, "", CALLBACK_PATH, Duration.ZERO).toString();
        if (state == null || expectedState == null || !MessageDigest.isEqual(
                state.getBytes(StandardCharsets.UTF_8), expectedState.getBytes(StandardCharsets.UTF_8))) {
            log.warn("Rejected EVE SSO callback with missing or mismatched state");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(HttpHeaders.SET_COOKIE, clearState)
                    .contentType(MediaType.TEXT_HTML)
                    .body("<p>Login failed or expired. Please close this window and try again.</p>");
        }

        OAuthToken savedToken = tokenService.exchangeCodeForTokens(code);
        String session = sessionService.create(savedToken.getAccountId());

        // Tell the opener we're done (it also polls for the popup closing), then close the popup
        String script = "<script>"
                + "if (window.opener) { window.opener.postMessage('eve-login-complete', '" + frontendBaseUrl + "'); }"
                + "window.close();</script>";
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, SessionService.sessionCookie(session).toString())
                .header(HttpHeaders.SET_COOKIE, clearState)
                .contentType(MediaType.TEXT_HTML)
                .body(script);
    }

    @GetMapping("/auth/me")
    public ResponseEntity<?> getUserInfo(Authentication auth) {
        CharResult character = characterService.getCharacter(auth.getName());
        if (character == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Session expired or invalid");
        }
        return ResponseEntity.ok(Map.of("character", character));
    }

    @PostMapping("/auth/extend-session")
    public ResponseEntity<?> extendSession(@CookieValue(value = SessionService.SESSION_COOKIE, required = false) String session,
                                           HttpServletResponse response) {
        if (!sessionService.extend(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid session, please log in again");
        }
        response.addHeader(HttpHeaders.SET_COOKIE, SessionService.sessionCookie(session).toString());
        return ResponseEntity.ok("Session extended");
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(@CookieValue(value = SessionService.SESSION_COOKIE, required = false) String session) {
        sessionService.delete(session);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, SessionService.clearedSessionCookie().toString())
                .body(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/auth/wallet")
    public ResponseEntity<BigDecimal> getWalletBalance(Authentication auth) {
        return ResponseEntity.ok(characterService.getWalletBalances(auth.getName()));
    }

    /**
     * Same redirect URI Spring Security's "{baseUrl}/api/v1/callback" template produced,
     * so the URL registered with the EVE developer app keeps matching.
     */
    private static String callbackUrl(HttpServletRequest request) {
        return UriComponentsBuilder.fromUriString(UrlUtils.buildFullRequestUrl(request))
                .replacePath(request.getContextPath() + CALLBACK_PATH)
                .replaceQuery(null)
                .fragment(null)
                .build()
                .toUriString();
    }
}
