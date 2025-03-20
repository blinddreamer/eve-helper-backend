package com.example.pandatribe.feign;

import com.example.pandatribe.feign.contracts.EveApiList;
import com.example.pandatribe.feign.contracts.EveInteractor;
import com.example.pandatribe.models.authentication.TokenRequest;
import com.example.pandatribe.models.characters.CharPortrait;
import com.example.pandatribe.models.characters.CharacterLoginInfo;
import com.example.pandatribe.models.industry.SystemCostIndexes;
import com.example.pandatribe.models.market.ItemPrice;
import com.example.pandatribe.models.market.MarketPriceData;
import com.example.pandatribe.models.authentication.RefreshTokenRequest;
import com.example.pandatribe.models.authentication.TokenResponse;
import com.example.pandatribe.utils.Helper;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class EveInteractorImpl implements EveInteractor {
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String BASIC_AUTH_PREFIX = "Basic ";
    @Value("${EVE_CLIENT_ID}")
    private String clientId;
    @Value("${EVE_CLIENT_SECRET}")
    private String clientSecret;
    private final FeignConfig feign;
    private final Helper helper;
    public static final String API_ADDRESS = "https://esi.evetech.net";
    public static final String AUTH_ADDRESS = "https://login.eveonline.com";


    @Override
    public TokenResponse requestAccessToken(TokenRequest request) {
        return feign.getRestClientWithAuthentication(EveApiList.class,AUTH_ADDRESS, helper.generateBasicAuthToken(clientId, clientSecret), BASIC_AUTH_PREFIX)
                .requestAccessToken(request.getGrantType(), request.getCode());
    }

    @Override
    public TokenResponse requestRefreshToken(RefreshTokenRequest request) {
        return feign.getRestClientWithAuthentication(EveApiList.class, AUTH_ADDRESS, helper.generateBasicAuthToken(clientId, clientSecret), BASIC_AUTH_PREFIX)
                .requestRefreshToken(request.getGrant_type(), request.getRefresh_token());
    }

    @Override
    public CharacterLoginInfo getCharacterLoginInfo(String accessToken) {
        return feign.getRestClientWithAuthentication(EveApiList.class, API_ADDRESS, accessToken, BEARER_PREFIX).getCharacterLoginInfo();
    }

    @Override
    public CharPortrait getCharPortrait(Integer characterId) {
        return feign.getRestClient(EveApiList.class, API_ADDRESS).getCharPortrait(characterId);
    }

    @Override
    public List<ItemPrice> getItemMarketPrice(Integer regionId, String dataSource, String orderType, Integer typeId) {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("datasource",dataSource);
        queryParams.put("order_type",orderType);
        if(Objects.nonNull(typeId)){
            queryParams.put("type_id",typeId);
        }
        return feign.getRestClient(EveApiList.class,API_ADDRESS).getMarketData(regionId, queryParams);
    }

    public List<MarketPriceData> getMarketPrices(){
        return feign.getRestClient(EveApiList.class,API_ADDRESS).getMarketPrices();
    }

    @Override
    public List<SystemCostIndexes> getSystemCostIndexes() {
        return feign.getRestClient(EveApiList.class, API_ADDRESS).getSystemCostIndexes();
    }
}
