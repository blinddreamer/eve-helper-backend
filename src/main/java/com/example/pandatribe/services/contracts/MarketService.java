package com.example.pandatribe.services.contracts;

import com.example.pandatribe.models.dbmodels.market.MarketHistoryEntity;
import com.example.pandatribe.models.dbmodels.market.MarketOrderEntity;
import com.example.pandatribe.models.market.EveMarketGroup;
import com.example.pandatribe.models.market.ItemPrice;
import com.example.pandatribe.models.market.MarketPriceData;
import com.example.pandatribe.models.market.MarketType;

import java.math.BigDecimal;
import java.util.List;

public interface MarketService {
    List<ItemPrice> getItemMarketPrice(Integer typeId, Integer regionId, String orderType);

    BigDecimal getItemSellOrderPrice(Integer locationId, List<ItemPrice> itemPriceList);

    List<MarketPriceData> getMarketPriceData();

    BigDecimal getItemPriceByOrderType(String orderType, List<ItemPrice> itemPriceList, Long locationId);

    List<MarketType> getMarketTypes();

    List<MarketOrderEntity> getMarketOrders(Integer typeId, Integer regionId);

    // Direct DB read with no freshness check — used by PI to avoid lock contention with the warmup scheduler
    List<MarketOrderEntity> getMarketOrdersFromDb(Integer typeId, Integer regionId);

    BigDecimal getItemPriceByOrderTypeFromOrders(String orderType, List<MarketOrderEntity> orders, Long locationId);

    List<MarketHistoryEntity> getMarketHistory(Integer typeId, Integer regionId);

    // In-game market group tree (invMarketGroups from SDE) — what the in-game Market browser shows,
    // as opposed to the raw inventory category/group tree.
    List<EveMarketGroup> getMarketGroups();

    List<MarketType> getMarketGroupTypes(Integer marketGroupId);
}
