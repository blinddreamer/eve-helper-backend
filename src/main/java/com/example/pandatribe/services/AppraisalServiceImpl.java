package com.example.pandatribe.services;

import com.example.pandatribe.models.dbmodels.appraisal.AppraisalData;
import com.example.pandatribe.models.industry.blueprints.EveType;
import com.example.pandatribe.models.market.ItemPrice;
import com.example.pandatribe.models.requests.AppraisalRequest;
import com.example.pandatribe.models.results.AppraisalResult;
import com.example.pandatribe.models.results.AppraisalResultEntity;
import com.example.pandatribe.repositories.interfaces.AppraisalDataRepository;
import com.example.pandatribe.repositories.interfaces.EveCustomRepository;
import com.example.pandatribe.repositories.interfaces.EveTypesRepository;
import com.example.pandatribe.services.contracts.AppraisalService;
import com.example.pandatribe.services.contracts.MarketService;
import com.example.pandatribe.utils.Helper;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@AllArgsConstructor
public class AppraisalServiceImpl implements AppraisalService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppraisalServiceImpl.class);
    public static final String ORDER_TYPE = "all";
    public static final Integer REGION_ID = 10000002;
    public static final Long DEFAULT_LOCATION_ID = Long.parseLong( "60003760");
    private final MarketService marketService;
    private final EveTypesRepository eveTypesRepository;
    private final EveCustomRepository eveCustomRepository;
    private final AppraisalDataRepository appraisalDataRepository;
    private final Helper helper;

    @Override
    public String generateAppraisalResult(AppraisalRequest appraisalRequest) {

     List<AppraisalResultEntity> appraisalEntities = appraisalRequest.getAppraisalRequestEntityList().stream().map(appraisal -> {
            EveType eveType = eveTypesRepository.findEveTypeByTypeName(appraisal.getName()).stream().findFirst().orElse(null);
            if(Objects.isNull(eveType)){
                LOGGER.error("Eve ITEM with name {} was not found",appraisal.getName());
                return null;
            }
         Integer volume = eveCustomRepository.getVolume(eveType.getTypeId());
                 Long locationId = Optional.ofNullable(appraisalRequest.getRegionId()).map(s-> s.split("_")[1]).map(Long::parseLong).orElse(DEFAULT_LOCATION_ID);
                 Integer regionId = Optional.ofNullable(appraisalRequest.getRegionId()).map(s-> s.split("_")[0]).map(Integer::parseInt).orElse(REGION_ID);
         List<ItemPrice> itemPriceList = marketService
                 .getItemMarketPrice(eveType.getTypeId(),regionId, ORDER_TYPE);
         BigDecimal buyOrderPrice = marketService.getItemPriceByOrderType("buy", itemPriceList,locationId);
         BigDecimal sellOrderPrice = marketService.getItemPriceByOrderType("sell", itemPriceList,locationId);
            return AppraisalResultEntity.builder()
                    .icon(helper.generateIconLink(eveType.getTypeId(),32))
                    .quantity(appraisal.getQuantity())
                    .volume(Objects.nonNull(volume) ? volume : eveType.getVolume())
                    .item(eveType.getTypeName())

                    .buyOrdersCount(itemPriceList.stream()
                            .filter(itemPrice -> itemPrice.getIsBuyOrder().equals(true))
                            .count())
                    .buyOrderPrice(buyOrderPrice)
                    .sellOrderPrice(sellOrderPrice)
                    .splitPrice(buyOrderPrice.add(sellOrderPrice).divide(BigDecimal.valueOf(2)).setScale(0, RoundingMode.CEILING))
                    .sellOrdersCount(itemPriceList.stream()
                            .filter(itemPrice -> itemPrice.getIsBuyOrder().equals(false))
                            .count())
                    .build();
                })
             .toList();


        AppraisalResult appraisalResult =  AppraisalResult.builder()
                .appraisals(appraisalEntities)
                .regionId(appraisalRequest.getRegionId())
                .estimateTotalBuy(appraisalEntities.stream().map(a -> a.getBuyOrderPrice().multiply(BigDecimal.valueOf(a.getQuantity()))).reduce(BigDecimal::add).orElse(BigDecimal.ZERO))
                .estimateTotalSell(appraisalEntities.stream().map(a -> a.getSellOrderPrice().multiply(BigDecimal.valueOf(a.getQuantity()))).reduce(BigDecimal::add).orElse(BigDecimal.ZERO))
                .estimateTotalSplit(appraisalEntities.stream().map(a -> a.getSplitPrice().multiply(BigDecimal.valueOf(a.getQuantity()))).reduce(BigDecimal::add).orElse(BigDecimal.ZERO))
                .totalVolume(appraisalEntities.stream().map(a -> a.getVolume()*a.getQuantity()).reduce(Double::sum).orElse(0.0))
                .build();
        UUID uuid = UUID.randomUUID();
        String shortenLink = helper.compressUUID(uuid);
        appraisalDataRepository.saveAndFlush(AppraisalData.builder().id(uuid).appraisalResult(appraisalResult).creationDate(new Date())
                        .comment(appraisalRequest.getComment())
                        .system(appraisalRequest.getSystem())
                        .market(appraisalRequest.getRegionId())
                        .pricePercentage(appraisalRequest.getPricePercentage())
                        .transactionType(appraisalRequest.getTransactionType())
                .build());

        return shortenLink;
    }

    public AppraisalData getAppraisalResult(String id) {
        UUID uuid = helper.decompressUUID(id);
        Optional<AppraisalData> appraisalData = appraisalDataRepository.findById(uuid);
        if(appraisalData.isPresent()){
            return appraisalData.get();
        }
        throw new IllegalArgumentException("Appraisal data not found");
    }
}
