package com.example.pandatribe.services;

import com.example.pandatribe.models.dbmodels.appraisal.AppraisalData;
import com.example.pandatribe.models.industry.blueprints.EveType;
import com.example.pandatribe.models.industry.blueprints.InvTypeMaterial;
import com.example.pandatribe.models.market.ItemPrice;
import com.example.pandatribe.models.requests.AppraisalRequest;
import com.example.pandatribe.models.results.*;
import com.example.pandatribe.repositories.BlueprintQueryRepository;
import com.example.pandatribe.repositories.interfaces.AppraisalDataRepository;
import com.example.pandatribe.repositories.interfaces.EveTypesRepository;
import com.example.pandatribe.repositories.interfaces.InvTypeMaterialRepository;
import com.example.pandatribe.services.contracts.AppraisalService;
import com.example.pandatribe.services.contracts.MarketService;
import com.example.pandatribe.utils.EncodingUtil;
import com.example.pandatribe.utils.EveImageService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AppraisalServiceImpl implements AppraisalService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppraisalServiceImpl.class);
    public static final String ORDER_TYPE = "all";
    public static final Integer REGION_ID = 10000002;
    public static final Long DEFAULT_LOCATION_ID = Long.parseLong( "60003760");
    private final MarketService marketService;
    private final EveTypesRepository eveTypesRepository;
    private final BlueprintQueryRepository blueprintQueryRepository;
    private final AppraisalDataRepository appraisalDataRepository;
    private final InvTypeMaterialRepository invTypeMaterialRepository;
    private final EveImageService eveImageService;
    private final EncodingUtil encodingUtil;

    @Override
    public String generateAppraisalResult(AppraisalRequest appraisalRequest) {

     List<AppraisalResultEntity> appraisalEntities = appraisalRequest.getAppraisalRequestEntityList().stream().map(appraisal -> {
            EveType eveType = eveTypesRepository.findEveTypeByTypeName(appraisal.getName()).stream().findFirst().orElse(null);
            if(Objects.isNull(eveType)){
                LOGGER.warn("Eve item not found, skipping: '{}'", appraisal.getName());
                return null;
            }
         Double volume = blueprintQueryRepository.getVolume(eveType.getTypeId());
                 Long locationId = Optional.ofNullable(appraisalRequest.getRegionId()).map(s-> s.split("_")[1]).map(Long::parseLong).orElse(DEFAULT_LOCATION_ID);
                 Integer regionId = Optional.ofNullable(appraisalRequest.getRegionId()).map(s-> s.split("_")[0]).map(Integer::parseInt).orElse(REGION_ID);
         List<ItemPrice> itemPriceList = marketService
                 .getItemMarketPrice(eveType.getTypeId(),regionId, ORDER_TYPE);
         BigDecimal buyOrderPrice = marketService.getItemPriceByOrderType("buy", itemPriceList,locationId);
         BigDecimal sellOrderPrice = marketService.getItemPriceByOrderType("sell", itemPriceList,locationId);
            return AppraisalResultEntity.builder()
                    .icon(eveImageService.generateIconLink(eveType.getTypeId(),32))
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
             .filter(Objects::nonNull)
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
        String shortenLink = encodingUtil.compressUUID(uuid);
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
        UUID uuid = encodingUtil.decompressUUID(id);
        Optional<AppraisalData> appraisalData = appraisalDataRepository.findById(uuid);
        if(appraisalData.isPresent()){
            return appraisalData.get();
        }
        throw new IllegalArgumentException("Appraisal data not found");
    }

    @Override
    public ReprocessResult getReprocessResult(String id, Double efficiency) {
        AppraisalData appraisalData = getAppraisalResult(id);
        Long locationId = resolveLocationId(appraisalData.getMarket());
        Integer regionId = resolveRegionId(appraisalData.getMarket());

        // Aggregate yields across all items
        Map<Integer, Long> materialTotals = new HashMap<>();
        appraisalData.getAppraisalResult().getAppraisals().forEach(entity -> {
            EveType type = eveTypesRepository.findEveTypeByTypeName(entity.getItem())
                    .stream().findFirst().orElse(null);
            if (type == null) return;

            int portionSize = type.getPortionSize() != null ? type.getPortionSize() : 1;
            long batches = (long) entity.getQuantity() / portionSize;
            if (batches == 0) return;

            invTypeMaterialRepository.findByIdTypeId(type.getTypeId()).forEach(mat -> {
                long yield = Math.round(batches * mat.getQuantity() * efficiency);
                materialTotals.merge(mat.getId().getMaterialTypeId(), yield, Long::sum);
            });
        });

        List<ReprocessEntry> entries = materialTotals.entrySet().stream()
                .map(e -> {
                    EveType matType = eveTypesRepository.findEveTypeByTypeId(e.getKey()).orElse(null);
                    if (matType == null) return null;
                    List<ItemPrice> prices = marketService.getItemMarketPrice(e.getKey(), regionId, ORDER_TYPE);
                    BigDecimal buy  = marketService.getItemPriceByOrderType("buy",  prices, locationId);
                    BigDecimal sell = marketService.getItemPriceByOrderType("sell", prices, locationId);
                    return ReprocessEntry.builder()
                            .typeId(e.getKey())
                            .name(matType.getTypeName())
                            .icon(eveImageService.generateIconLink(e.getKey(), 32))
                            .quantity(e.getValue())
                            .sellPrice(sell)
                            .buyPrice(buy)
                            .totalSell(sell.multiply(BigDecimal.valueOf(e.getValue())))
                            .totalBuy(buy.multiply(BigDecimal.valueOf(e.getValue())))
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ReprocessEntry::getName))
                .collect(Collectors.toList());

        BigDecimal totalSell  = entries.stream().map(ReprocessEntry::getTotalSell).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalBuy   = entries.stream().map(ReprocessEntry::getTotalBuy).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSplit = totalSell.add(totalBuy).divide(BigDecimal.valueOf(2), 0, RoundingMode.CEILING);

        return ReprocessResult.builder()
                .efficiency(efficiency)
                .materials(entries)
                .totalSell(totalSell)
                .totalBuy(totalBuy)
                .totalSplit(totalSplit)
                .build();
    }

    @Override
    public CompressResult getCompressResult(String id) {
        AppraisalData appraisalData = getAppraisalResult(id);
        Long locationId = resolveLocationId(appraisalData.getMarket());
        Integer regionId = resolveRegionId(appraisalData.getMarket());

        List<CompressEntry> entries = appraisalData.getAppraisalResult().getAppraisals().stream()
                .map(entity -> {
                    EveType type = eveTypesRepository.findEveTypeByTypeName(entity.getItem())
                            .stream().findFirst().orElse(null);
                    if (type == null) return null;

                    EveType compressed = eveTypesRepository
                            .findEveTypeByTypeName("Compressed " + type.getTypeName())
                            .stream().findFirst().orElse(null);
                    if (compressed == null) return null;

                    int portionSize = type.getPortionSize() != null ? type.getPortionSize() : 100;
                    long compressedQty = (long) entity.getQuantity() / portionSize;
                    if (compressedQty == 0) return null;
                    long remainder = (long) entity.getQuantity() % portionSize;

                    List<ItemPrice> origPrices = marketService.getItemMarketPrice(type.getTypeId(), regionId, ORDER_TYPE);
                    List<ItemPrice> compPrices  = marketService.getItemMarketPrice(compressed.getTypeId(), regionId, ORDER_TYPE);

                    double origVol = (type.getVolume() != null ? type.getVolume() : 0.0) * entity.getQuantity();
                    double compVol = (compressed.getVolume() != null ? compressed.getVolume() : 0.0) * compressedQty;

                    return CompressEntry.builder()
                            .originalName(type.getTypeName())
                            .originalQuantity((long) entity.getQuantity())
                            .originalVolume(origVol)
                            .remainder(remainder)
                            .compressedName(compressed.getTypeName())
                            .compressedTypeId(compressed.getTypeId())
                            .compressedIcon(eveImageService.generateIconLink(compressed.getTypeId(), 32))
                            .compressedQuantity(compressedQty)
                            .compressedVolume(compVol)
                            .volumeSaved(origVol - compVol)
                            .originalSellPrice(marketService.getItemPriceByOrderType("sell", origPrices, locationId))
                            .originalBuyPrice(marketService.getItemPriceByOrderType("buy",  origPrices, locationId))
                            .compressedSellPrice(marketService.getItemPriceByOrderType("sell", compPrices, locationId))
                            .compressedBuyPrice(marketService.getItemPriceByOrderType("buy",  compPrices, locationId))
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        double totalOrig = entries.stream().mapToDouble(CompressEntry::getOriginalVolume).sum();
        double totalComp = entries.stream().mapToDouble(CompressEntry::getCompressedVolume).sum();

        return CompressResult.builder()
                .items(entries)
                .totalOriginalVolume(totalOrig)
                .totalCompressedVolume(totalComp)
                .totalVolumeSaved(totalOrig - totalComp)
                .build();
    }

    private Long resolveLocationId(String market) {
        return Optional.ofNullable(market).map(s -> s.split("_")[1]).map(Long::parseLong).orElse(DEFAULT_LOCATION_ID);
    }

    private Integer resolveRegionId(String market) {
        return Optional.ofNullable(market).map(s -> s.split("_")[0]).map(Integer::parseInt).orElse(REGION_ID);
    }
}
