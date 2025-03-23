package com.example.pandatribe.services;

import com.example.pandatribe.feign.contracts.EveInteractor;
import com.example.pandatribe.models.market.ItemPrice;
import com.example.pandatribe.models.market.MarketPriceData;
import com.example.pandatribe.repositories.interfaces.ItemPriceRepository;
import com.example.pandatribe.services.contracts.MarketService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class MarketServiceImpl implements MarketService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarketServiceImpl.class);
    private final ItemPriceRepository itemPriceRepository;
    private final EveInteractor eveInteractor;
    public static final String DATA_SOURCE = "tranquility";



    @Override
    @Cacheable(value = "cacheItemMarketPrice")
    public List<ItemPrice> getItemMarketPrice(Integer typeId, Integer regionId , String orderType) {
        List<ItemPrice> itemPriceList = eveInteractor.getItemMarketPrice(regionId,DATA_SOURCE,orderType,typeId) ;
        LOGGER.info("Item {} prices obtained {}", typeId, !itemPriceList.isEmpty());
        return itemPriceList;
    }

    @Override
    public BigDecimal getItemSellOrderPrice(Integer locationId, List<ItemPrice> itemPriceList) {
        return itemPriceList.stream()
               // .filter(itemPrice -> Objects.equals(itemPrice.getLocationId(), locationId))
                .map(ItemPrice::getPrice)
                .sorted()
                .findFirst().orElse(BigDecimal.ZERO);

    }
    public BigDecimal getItemPriceByOrderType(String orderType, List<ItemPrice> itemPriceList, Long locationId) {
        if (orderType.equals("buy")) {
           return itemPriceList.stream()
                   .filter(itemPrice -> Objects.equals(itemPrice.getLocationId(), locationId))
                   .filter(itemPrice -> Objects.equals(itemPrice.getIsBuyOrder(), true))
                   .map(ItemPrice::getPrice).max(BigDecimal::compareTo)
                   .orElse(BigDecimal.ZERO);
        }
       return itemPriceList.stream()
                .filter(itemPrice -> Objects.equals(itemPrice.getLocationId(), locationId))
                .filter(itemPrice ->  Objects.equals(itemPrice.getIsBuyOrder(), false))
                .map(ItemPrice::getPrice)
                .sorted()
                .findFirst()
               .orElse(BigDecimal.ZERO);
    }
    @Override
    @Cacheable("cacheMarketPrices")
    public List<MarketPriceData> getMarketPriceData(){
        List<MarketPriceData> marketPriceDataList = eveInteractor.getMarketPrices();
        LOGGER.info("All market prices data is obtained: {}", !marketPriceDataList.isEmpty());
        return marketPriceDataList;
    }
}
