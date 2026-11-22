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
        final int batchSize = 1000;
        for (int i = 0; i < uniqueIds.size(); i += batchSize) {
            List<Integer> batch = uniqueIds.subList(i, Math.min(i + batchSize, uniqueIds.size()));
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
        if (name == null) {
            return false;
        }
        // T2 blueprints — items ending with " II Blueprint" (modules, drones, etc.)
        // and "Advanced * Blueprint" (T2 ammo/charge blueprints)
        return name.endsWith(" II Blueprint") || name.endsWith(" II Blueprint Copy")
                || name.startsWith("Advanced ") && (name.endsWith(" Blueprint") || name.endsWith(" Blueprint Copy"))
                // Debug / test / QA items
                || name.startsWith("DO NOT TRANSLATE")
                || name.startsWith("QA ")
                || name.startsWith("Test Server")
                // Event/promotional items
                || name.startsWith("ASI 2018")
                // Expert Systems (limited-time skill injectors, not regular market)
                || name.contains("Expert System")
                // Ship Emblems (cosmetic only)
                || name.contains("Ship Emblem")
                // Planetary industry (non-tradeable PI infrastructure)
                || name.contains("Planetary Industry");
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
