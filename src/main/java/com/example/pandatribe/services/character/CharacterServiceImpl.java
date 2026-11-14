package com.example.pandatribe.services.character;

import com.example.pandatribe.feign.contracts.EveInteractor;
import com.example.pandatribe.models.dbmodels.auth.OAuthToken;
import com.example.pandatribe.models.dbmodels.character.CharacterData;
import com.example.pandatribe.models.results.CharResult;
import com.example.pandatribe.repositories.interfaces.CharacterDataRepository;
import com.example.pandatribe.services.authentication.OAuthTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CharacterServiceImpl {
    private final EveInteractor eveInteractor;
    private final CharacterDataRepository characterDataRepository;
    private final OAuthTokenService oAuthTokenService;

    /** @return null if the account has no primary character (e.g. tokens were removed) */
    public CharResult getCharacter(String accountId) {
        OAuthToken oAuthToken = oAuthTokenService.findPrimaryCharacter(accountId);
        if (oAuthToken == null) {
            return null;
        }
        CharacterData csData = characterDataRepository.findById(oAuthToken.getCharacterId()).orElse(null);
        if (csData == null) {
            return null;
        }
        return CharResult.builder().charId(csData.getCharId()).name(csData.getCharName()).avatar(csData.getAvatar()).build();
    }

    public BigDecimal getWalletBalances(String id) {
        List<OAuthToken> allAccountTokens = oAuthTokenService.getAllCharacterTokensByAccountId(id);
       return allAccountTokens.stream().map(token -> oAuthTokenService.getValidAccessToken(token.getCharacterId()))
                .map(token-> eveInteractor.getWalletBalance(token.getCharacterId(), token.getAccessToken()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
