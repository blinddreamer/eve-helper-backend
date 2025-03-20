package com.example.pandatribe.feign.contracts;

import com.example.pandatribe.models.authentication.TokenResponse;
import com.example.pandatribe.models.characters.CharPortrait;
import com.example.pandatribe.models.characters.CharacterLoginInfo;
import com.example.pandatribe.models.industry.SystemCostIndexes;
import com.example.pandatribe.models.market.ItemPrice;
import com.example.pandatribe.models.market.MarketPriceData;
import feign.*;

import java.util.List;
import java.util.Map;


@Headers({"Accept: application/json",
"Content-type: application/json"})
public interface EveApiList {

    @RequestLine("POST /v2/oauth/token")
    @Headers({"Content-Type: application/x-www-form-urlencoded"})
    @Body("grant_type={grant_type}&code={code}")
    TokenResponse requestAccessToken(@Param("grant_type") String grantType, @Param ("code") String code);

    @RequestLine("POST /v2/oauth/token")
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @Body("grant_type={grant_type}&refresh_token={refresh_token}")
    TokenResponse requestRefreshToken(@Param("grant_type") String grantType,
                                      @Param("refresh_token") String refreshToken);

    @RequestLine("GET /verify")
    CharacterLoginInfo getCharacterLoginInfo();

    @RequestLine("GET /latest/characters/{characterId}/portrait")
    CharPortrait getCharPortrait(@Param("characterId") Integer characterId);

    @RequestLine("GET /latest/markets/{regionId}/orders/?datasource={dataSource}&order_type={orderType}&type_id={typeId}")
    List<ItemPrice> getMarketData(@Param("regionId") Integer regionId, @QueryMap Map<String,Object> queryParams);

    @RequestLine("GET /latest/markets/prices")
    List<MarketPriceData> getMarketPrices();

    @RequestLine("GET /latest/industry/systems")
    List<SystemCostIndexes> getSystemCostIndexes();
}
