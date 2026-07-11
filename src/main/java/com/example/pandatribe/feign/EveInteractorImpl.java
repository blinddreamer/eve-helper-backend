package com.example.pandatribe.feign;

import com.example.pandatribe.feign.contracts.EveApiList;
import com.example.pandatribe.feign.contracts.EveInteractor;
import com.example.pandatribe.feign.contracts.ExternalApi;
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
import com.example.pandatribe.models.universe.UniverseNameEntry;
import com.example.pandatribe.utils.AuthTokenUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EveInteractorImpl implements EveInteractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(EveInteractorImpl.class);
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String BASIC_AUTH_PREFIX = "Basic ";
    @Value("${EVE_CLIENT_ID}")
    private String clientId;
    @Value("${EVE_CLIENT_SECRET}")
    private String clientSecret;
    private final FeignConfig feign;
    private final AuthTokenUtil authTokenUtil;
    private final ObjectMapper objectMapper;
    private static final Integer JITA_REGION_ID = 10000002;
    public static final String API_ADDRESS = "https://esi.evetech.net";
    @Value("${NTFY_SELECTED_ADDRESS}")
    private String ntfyAddress;
    @Value("${NTFY_SELECTED_TOKEN}")
    private String ntfyApiToken;
    public static final String AUTH_ADDRESS = "https://login.eveonline.com";


    @Override
    public TokenResponse requestAccessToken(TokenRequest request) {
        return feign.getRestClientWithAuthentication(EveApiList.class,AUTH_ADDRESS, authTokenUtil.generateBasicAuthToken(clientId, clientSecret), BASIC_AUTH_PREFIX)
                .requestAccessToken(request.getGrantType(), request.getCode());
    }

    @Override
    public TokenResponse requestRefreshToken(RefreshTokenRequest request) {
        return feign.getRestClientWithAuthentication(EveApiList.class, AUTH_ADDRESS, authTokenUtil.generateBasicAuthToken(clientId, clientSecret), BASIC_AUTH_PREFIX)
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

    @Override
    public BigDecimal getWalletBalance(Integer characterId, String accessToken) {
        return feign.getRestClientWithAuthentication(EveApiList.class, API_ADDRESS, accessToken, BEARER_PREFIX)
                .getWalletBalance(characterId);
    }

    @Override
    public List<MarketType> getMarketTypes() {
        EveApiList client = feign.getRestClient(EveApiList.class, API_ADDRESS);

        // Fetch page 1 and read X-Pages header
        List<Integer> allIds = new ArrayList<>();
        int totalPages;
        try (Response firstPage = client.getMarketTypeIds(JITA_REGION_ID, 1)) {
            totalPages = Optional.ofNullable(firstPage.headers().get("X-Pages"))
                    .flatMap(h -> h.stream().findFirst())
                    .map(Integer::parseInt)
                    .orElse(1);
            allIds.addAll(parseTypeIds(firstPage));
        }

        // Fetch remaining pages sequentially
        for (int page = 2; page <= totalPages; page++) {
            try (Response pageResponse = client.getMarketTypeIds(JITA_REGION_ID, page)) {
                allIds.addAll(parseTypeIds(pageResponse));
            }
        }

        // Deduplicate
        List<Integer> uniqueIds = new ArrayList<>(new HashSet<>(allIds));

        // Resolve names in batches of 1000
        List<MarketType> result = new ArrayList<>();
        final int BATCH = 1000;
        for (int i = 0; i < uniqueIds.size(); i += BATCH) {
            List<Integer> batch = uniqueIds.subList(i, Math.min(i + BATCH, uniqueIds.size()));
            client.resolveUniverseNames(batch).stream()
                    .filter(n -> "inventory_type".equals(n.getCategory()))
                    .filter(n -> !isJunkMarketItem(n.getName()))
                    .map(n -> new MarketType(n.getId(), n.getName()))
                    .forEach(result::add);
        }

        result.sort(Comparator.comparing(MarketType::getName));
        LOGGER.info("Market type index built: {} tradeable types", result.size());
        return result;
    }

    private static boolean isJunkMarketItem(String name) {
        if (name == null) return false;
        // T2 blueprints — items ending with " II Blueprint" (modules, drones, etc.)
        // and "Advanced * Blueprint" (T2 ammo/charge blueprints)
        if (name.endsWith(" II Blueprint") || name.endsWith(" II Blueprint Copy")) return true;
        if (name.startsWith("Advanced ") && (name.endsWith(" Blueprint") || name.endsWith(" Blueprint Copy"))) return true;
        // Debug / test / QA items
        if (name.startsWith("DO NOT TRANSLATE")) return true;
        if (name.startsWith("QA ")) return true;
        if (name.startsWith("Test Server")) return true;
        // Event/promotional items
        if (name.startsWith("ASI 2018")) return true;
        // Expert Systems (limited-time skill injectors, not regular market)
        if (name.contains("Expert System")) return true;
        // Ship Emblems (cosmetic only)
        if (name.contains("Ship Emblem")) return true;
        // Planetary industry (non-tradeable PI infrastructure)
        if (name.contains("Planetary Industry")) return true;
        return false;
    }

    private List<Integer> parseTypeIds(Response response) {
        try {
            return objectMapper.readValue(
                    response.body().asInputStream(),
                    new TypeReference<List<Integer>>() {}
            );
        } catch (Exception e) {
            // Propagate instead of returning an empty list — callers must not mistake
            // a parse failure for "ESI has zero results" (that previously caused
            // MarketOrderRefresher to wipe valid cached data on a transient error).
            throw new RuntimeException("Failed to parse market type IDs from ESI response", e);
        }
    }

    @Override
    public List<ItemPrice> getMarketOrders(Integer typeId, Integer regionId) {
        EveApiList client = feign.getRestClient(EveApiList.class, API_ADDRESS);
        List<ItemPrice> allOrders = new ArrayList<>();
        int totalPages;
        try (Response firstPage = client.getMarketOrdersPage(regionId, typeId, 1)) {
            totalPages = Optional.ofNullable(firstPage.headers().get("X-Pages"))
                    .flatMap(h -> h.stream().findFirst())
                    .map(Integer::parseInt)
                    .orElse(1);
            allOrders.addAll(parseItemPrices(firstPage));
        }
        for (int page = 2; page <= totalPages; page++) {
            try (Response pageResponse = client.getMarketOrdersPage(regionId, typeId, page)) {
                allOrders.addAll(parseItemPrices(pageResponse));
            }
        }
        LOGGER.info("Fetched {} orders from ESI for typeId={} regionId={}", allOrders.size(), typeId, regionId);
        return allOrders;
    }

    private List<ItemPrice> parseItemPrices(Response response) {
        try {
            return objectMapper.readValue(
                    response.body().asInputStream(),
                    new TypeReference<List<ItemPrice>>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse market orders from ESI response", e);
        }
    }

    @Override
    public List<EsiMarketHistory> getMarketHistory(Integer regionId, Integer typeId) {
        return feign.getRestClient(EveApiList.class, API_ADDRESS).getMarketHistory(regionId, typeId);
    }

    @Override
    public void sendNotification(String topic, String title, String message, String priority) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Title", title);
        headers.put("Priority", priority);
        feign.getRestClientWithAuthentication(ExternalApi.class, ntfyAddress, ntfyApiToken, BEARER_PREFIX)
                .sendNotification(topic, headers, message);
    }
}
