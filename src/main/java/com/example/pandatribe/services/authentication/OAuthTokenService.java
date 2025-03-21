package com.example.pandatribe.services.authentication;

import com.example.pandatribe.feign.contracts.EveInteractor;
import com.example.pandatribe.models.dbmodels.auth.OAuthToken;
import com.example.pandatribe.models.authentication.RefreshTokenRequest;
import com.example.pandatribe.models.authentication.TokenRequest;
import com.example.pandatribe.models.authentication.TokenResponse;
import com.example.pandatribe.models.characters.CharPortrait;
import com.example.pandatribe.models.dbmodels.character.CharacterData;
import com.example.pandatribe.models.characters.CharacterLoginInfo;
import com.example.pandatribe.repositories.interfaces.CharacterDataRepository;
import com.example.pandatribe.repositories.interfaces.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuthTokenService {


    private final TokenRepository tokenRepository;
    private final CharacterDataRepository characterDataRepository;
    private final EveInteractor eveInteractor;

    @Transactional
    public OAuthToken exchangeCodeForTokens(String code) {
        TokenRequest tokenRequest = TokenRequest.builder()
                .grantType("authorization_code")
                .code(code)
                .build();
        TokenResponse tokenResponse = eveInteractor.requestAccessToken(tokenRequest);
        //   Get character info
        CharacterLoginInfo character = eveInteractor.getCharacterLoginInfo(tokenResponse.getAccessToken());
        CharPortrait charPortrait = eveInteractor.getCharPortrait(character.getCharacterId());
        CharacterData csData = CharacterData.builder()
                .charId(character.getCharacterId())
                .charName(character.getName())
                .avatar(charPortrait.getPx64x64())
                .build();
        characterDataRepository.save(csData);

        OAuthToken oAuthToken = OAuthToken.builder()
                .accessToken(tokenResponse.getAccessToken())
                .id(UUID.randomUUID().toString())
                .refreshToken(tokenResponse.getRefreshToken())
                .expiresAt(Instant.now().plus(Duration.ofSeconds(tokenResponse.getExpiresIn())))
                .characterId(character.getCharacterId())
                .build();
        return tokenRepository.save(oAuthToken);
    }

    public OAuthToken checkTokenExist(String token) {
        OAuthToken oAuthToken = tokenRepository.findById(token).orElse(null);
        return oAuthToken;
    }

    @Transactional
    public String getValidAccessToken(String id) {
        OAuthToken token = tokenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (Instant.now().isAfter(token.getExpiresAt())) {
            return refreshAccessToken(token);
        }

        return token.getAccessToken();
    }

    private String refreshAccessToken(OAuthToken token) {
        // Call EVE API to refresh the token
        TokenResponse response = eveInteractor.requestRefreshToken(RefreshTokenRequest.builder()
                        .grant_type("refresh_token")
                        .refresh_token(token.getRefreshToken())
                        .build());

        String newAccessToken = response.getAccessToken();
        String newRefreshToken = response.getRefreshToken();
        Integer expiresIn = response.getExpiresIn();
        token = token.withAccessToken(newAccessToken);
        token = token.withRefreshToken(newRefreshToken);
        token = token.withExpiresAt(Instant.now().plus(Duration.ofSeconds(expiresIn)));

        tokenRepository.save(token);
        return newAccessToken;
    }
}
