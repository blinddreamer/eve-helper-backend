package com.example.pandatribe.services;

import com.example.pandatribe.models.industry.blueprints.EveType;
import com.example.pandatribe.models.market.ItemPrice;
import com.example.pandatribe.models.requests.AppraisalRequest;
import com.example.pandatribe.models.results.AppraisalResult;
import com.example.pandatribe.models.results.AppraisalResultEntity;
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
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class AppraisalServiceImpl implements AppraisalService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppraisalServiceImpl.class);
    public static final String ORDER_TYPE = "all";
    private final MarketService marketService;
    private final EveTypesRepository eveTypesRepository;
    private final EveCustomRepository eveCustomRepository;
    private final Helper helper;

    @Override
    public AppraisalResult generateAppraisalResult(AppraisalRequest appraisalRequest) {

     List<AppraisalResultEntity> appraisalEntities = appraisalRequest.getAppraisalRequestEntityList().stream().map(appraisal -> {
            EveType eveType = eveTypesRepository.findEveTypeByTypeName(appraisal.getName()).orElse(null);
            if(Objects.isNull(eveType)){
                LOGGER.error("Eve ITEM with name {} was not found",appraisal.getName());
                return null;
            }
         Integer volume = eveCustomRepository.getVolume(eveType.getTypeId());
         List<ItemPrice> itemPriceList = marketService
                 .getItemMarketPrice(eveType.getTypeId(),appraisalRequest.getRegionId(), ORDER_TYPE);

            return AppraisalResultEntity.builder()
                    .icon(helper.generateIconLink(eveType.getTypeId(),32))
                    .quantity(appraisal.getQuantity())
                    .volume(Objects.nonNull(volume) ? volume : eveType.getVolume())
                    .item(eveType.getTypeName())
                  //  .buyOrderAverage()
                    .buyOrdersCount(itemPriceList.stream()
                            .filter(itemPrice -> itemPrice.getIsBuyOrder().equals(true))
                            .count())
                    .buyOrderPrice(marketService.getItemPriceByOrderType("buy", itemPriceList))
                    .sellOrderPrice(marketService.getItemPriceByOrderType("sell", itemPriceList))
                //    .sellOrderAverage()
                    .sellOrdersCount(itemPriceList.stream()
                            .filter(itemPrice -> itemPrice.getIsBuyOrder().equals(false))
                            .count())
                    .build();
                })
             .toList();


        return AppraisalResult.builder()
                .appraisals(appraisalEntities)
                .regionId(appraisalRequest.getRegionId())
                .estimateTotalBuy(appraisalEntities.stream().map(a -> a.getBuyOrderPrice().multiply(BigDecimal.valueOf(a.getQuantity()))).reduce(BigDecimal::add).orElse(BigDecimal.ZERO))
                .estimateTotalSell(appraisalEntities.stream().map(a -> a.getSellOrderPrice().multiply(BigDecimal.valueOf(a.getQuantity()))).reduce(BigDecimal::add).orElse(BigDecimal.ZERO))
                .totalVolume(appraisalEntities.stream().map(a -> a.getVolume()*a.getQuantity()).reduce(Double::sum).orElse(0.0))
                .build();
    }
}
