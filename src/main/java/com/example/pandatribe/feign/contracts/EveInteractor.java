package com.example.pandatribe.feign.contracts;

import com.example.pandatribe.models.authentication.RefreshTokenRequest;
import com.example.pandatribe.models.authentication.TokenRequest;
import com.example.pandatribe.models.authentication.TokenResponse;
import com.example.pandatribe.models.characters.CharPortrait;
import com.example.pandatribe.models.characters.CharacterLoginInfo;
import com.example.pandatribe.models.industry.SystemCostIndexes;
import com.example.pandatribe.models.market.EsiMarketHistory;
import com.example.pandatribe.models.market.ItemPrice;
import com.example.pandatribe.models.market.MarketPriceData;
import com.example.pandatribe.models.market.MarketType;

import java.math.BigDecimal;
import java.util.List;

public interface EveInteractor {
    TokenResponse requestAccessToken(TokenRequest request);

    TokenResponse requestRefreshToken(RefreshTokenRequest request);

    CharacterLoginInfo getCharacterLoginInfo(String accessToken);

    CharPortrait getCharPortrait(Integer characterId);

    List<ItemPrice> getItemMarketPrice(Integer regionId, String dataSource, String orderType, Integer typeId);

    List<MarketPriceData> getMarketPrices();

    List<SystemCostIndexes> getSystemCostIndexes();

    BigDecimal getWalletBalance(Integer characterId, String accessToken);

    void sendNotification(String topic, String title, String message, String priority);

    List<MarketType> getMarketTypes();

    List<ItemPrice> getMarketOrders(Integer typeId, Integer regionId);

    List<EsiMarketHistory> getMarketHistory(Integer regionId, Integer typeId);
}
