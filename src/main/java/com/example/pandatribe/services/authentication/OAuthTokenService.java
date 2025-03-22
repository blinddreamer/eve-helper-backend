package com.example.pandatribe.services.authentication;

import com.example.pandatribe.feign.contracts.EveInteractor;
import com.example.pandatribe.models.authentication.RefreshTokenRequest;
import com.example.pandatribe.models.authentication.TokenRequest;
import com.example.pandatribe.models.authentication.TokenResponse;
import com.example.pandatribe.models.characters.CharPortrait;
import com.example.pandatribe.models.characters.CharacterLoginInfo;
import com.example.pandatribe.models.dbmodels.auth.OAuthToken;
import com.example.pandatribe.models.dbmodels.character.CharacterData;
import com.example.pandatribe.repositories.interfaces.CharacterDataRepository;
import com.example.pandatribe.repositories.interfaces.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
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
        Boolean isPrimary = Boolean.TRUE;
        OAuthToken existing = tokenRepository.findOAuthTokenByCharacterId(character.getCharacterId()).orElse(null);
        if (existing != null) {
            isPrimary = Objects.isNull(findPrimaryCharacter(existing.getAccountId()));
        }

        OAuthToken oAuthToken = OAuthToken.builder()
                .accessToken(tokenResponse.getAccessToken())
                .accountId(Objects.isNull(existing) ? UUID.randomUUID().toString() : existing.getAccountId())
                .refreshToken(tokenResponse.getRefreshToken())
                .expiresAt(Instant.now().plus(Duration.ofSeconds(tokenResponse.getExpiresIn())))
                .characterId(character.getCharacterId())
                .isPrimary(isPrimary)
                .build();

        CharPortrait charPortrait = eveInteractor.getCharPortrait(character.getCharacterId());
        CharacterData csData = CharacterData.builder()
                .charId(character.getCharacterId())
                .charName(character.getName())
                .avatar(charPortrait.getPx64x64())
                .build();
        characterDataRepository.save(csData);
        return tokenRepository.save(oAuthToken);
    }

    public OAuthToken checkTokenExist(String token) {
        return tokenRepository.findOAuthTokenByAccountIdAndIsPrimary(token, Boolean.TRUE).orElse(null);
    }

    @Transactional
    public OAuthToken getValidAccessToken(Integer id) {
        OAuthToken token = tokenRepository.findOAuthTokenByCharacterId(id)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (Instant.now().isAfter(token.getExpiresAt())) {
            return refreshAccessToken(token);
        }

        return token;
    }

    public OAuthToken findPrimaryCharacter(String tokenId){
        return tokenRepository.findOAuthTokenByAccountIdAndIsPrimary(tokenId, Boolean.TRUE).orElse(null);
    }

    public List<OAuthToken> getAllCharacterTokensByAccountId(String accountId) {
        return tokenRepository.findAllByAccountId(accountId);
    }

    private OAuthToken refreshAccessToken(OAuthToken token) {
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

        return tokenRepository.save(token);
    }
}

