package com.example.pandatribe.controllers;

import com.example.pandatribe.models.dbmodels.auth.OAuthToken;
import com.example.pandatribe.models.results.CharResult;
import com.example.pandatribe.services.authentication.OAuthTokenService;
import com.example.pandatribe.services.character.CharacterServiceImpl;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EveAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OAuthTokenService tokenService;

    @MockitoBean
    private CharacterServiceImpl characterService;

    @Test
    void loginRedirectsToEveWithStateBoundToCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/login"))
                .andExpect(status().isFound())
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        assertThat(location).startsWith("https://login.eveonline.com/v2/oauth/authorize");
        var query = UriComponentsBuilder.fromUriString(location).build().getQueryParams();
        assertThat(query.getFirst("redirect_uri")).isEqualTo("http://localhost/api/v1/callback");
        Cookie stateCookie = result.getResponse().getCookie("oauth_state");
        assertThat(stateCookie).isNotNull();
        assertThat(query.getFirst("state")).isEqualTo(stateCookie.getValue());
    }

    @Test
    void callbackRejectsMismatchedState() throws Exception {
        mockMvc.perform(get("/api/v1/callback").param("code", "abc").param("state", "attacker")
                        .cookie(new Cookie("oauth_state", "victim")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/callback").param("code", "abc").param("state", "no-cookie"))
                .andExpect(status().isBadRequest());

        verify(tokenService, never()).exchangeCodeForTokens(anyString());
    }

    @Test
    void sessionCookieAuthenticatesUntilLogout() throws Exception {
        String accountId = UUID.randomUUID().toString();
        when(tokenService.exchangeCodeForTokens("good-code"))
                .thenReturn(OAuthToken.builder().accountId(accountId).characterId(1).build());
        when(characterService.getCharacter(accountId))
                .thenReturn(CharResult.builder().charId(1).name("Pilot").build());

        MvcResult callback = mockMvc.perform(get("/api/v1/callback").param("code", "good-code").param("state", "s1")
                        .cookie(new Cookie("oauth_state", "s1")))
                .andExpect(status().isOk())
                .andReturn();
        Cookie session = callback.getResponse().getCookie("session");
        assertThat(session).isNotNull();
        assertThat(session.isHttpOnly()).isTrue();
        assertThat(session.getValue()).doesNotContain(accountId); // random token, not the account id

        mockMvc.perform(get("/api/v1/auth/me").cookie(session)).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/logout").cookie(session)).andExpect(status().isOk());

        // Logout revokes the session server-side, so the old cookie value is now useless
        mockMvc.perform(get("/api/v1/auth/me").cookie(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsReturn401InsteadOfRedirecting() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me").cookie(new Cookie("session", "forged")))
                .andExpect(status().isUnauthorized());
        verify(characterService, never()).getCharacter(any());
    }
}
