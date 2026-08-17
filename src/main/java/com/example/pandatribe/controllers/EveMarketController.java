package com.example.pandatribe.controllers;

import com.example.pandatribe.models.dbmodels.market.MarketHistoryEntity;
import com.example.pandatribe.models.dbmodels.market.MarketOrderEntity;
import com.example.pandatribe.models.market.EveMarketGroup;
import com.example.pandatribe.models.market.MarketType;
import com.example.pandatribe.services.contracts.MarketService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/market")
public class EveMarketController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EveMarketController.class);
    private final MarketService marketService;

    @GetMapping("/types")
    public ResponseEntity<List<MarketType>> getMarketTypes() {
        LOGGER.debug("REQUEST for market type index received");
        return ResponseEntity.ok(marketService.getMarketTypes());
    }

    @GetMapping("/orders")
    public ResponseEntity<List<MarketOrderEntity>> getMarketOrders(
            @RequestParam Integer typeId,
            @RequestParam Integer regionId) {
        LOGGER.debug("REQUEST for market orders typeId={} regionId={}", typeId, regionId);
        return ResponseEntity.ok(marketService.getMarketOrders(typeId, regionId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<MarketHistoryEntity>> getMarketHistory(
            @RequestParam Integer typeId,
            @RequestParam Integer regionId) {
        LOGGER.debug("REQUEST for market history typeId={} regionId={}", typeId, regionId);
        return ResponseEntity.ok(marketService.getMarketHistory(typeId, regionId));
    }

    // In-game market group tree (mirrors the EVE client's Market browser), flat — parentGroupId
    // links each node to its parent. Frontend builds the nested tree from this.
    @GetMapping("/groups")
    public ResponseEntity<List<EveMarketGroup>> getMarketGroups() {
        LOGGER.debug("REQUEST for market group tree received");
        return ResponseEntity.ok(marketService.getMarketGroups());
    }

    @GetMapping("/groups/{marketGroupId}/types")
    public ResponseEntity<List<MarketType>> getMarketGroupTypes(@PathVariable Integer marketGroupId) {
        LOGGER.debug("REQUEST for market group types groupId={}", marketGroupId);
        return ResponseEntity.ok(marketService.getMarketGroupTypes(marketGroupId));
    }
}
