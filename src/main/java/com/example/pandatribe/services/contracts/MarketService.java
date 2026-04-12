package com.example.pandatribe.services.contracts;

import com.example.pandatribe.models.dbmodels.market.MarketHistoryEntity;
import com.example.pandatribe.models.dbmodels.market.MarketOrderEntity;
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

    List<MarketHistoryEntity> getMarketHistory(Integer typeId, Integer regionId);
}
