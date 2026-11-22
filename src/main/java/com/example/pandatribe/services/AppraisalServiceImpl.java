package com.example.pandatribe.services;

import com.example.pandatribe.models.dbmodels.appraisal.AppraisalData;
import com.example.pandatribe.models.industry.blueprints.EveType;
import com.example.pandatribe.models.dbmodels.market.MarketOrderEntity;
import com.example.pandatribe.configs.EveDataConfig;
import com.example.pandatribe.exceptions.InvalidRequestException;
import com.example.pandatribe.exceptions.RequestNotFoundException;
import com.example.pandatribe.models.requests.AppraisalRequest;
import com.example.pandatribe.models.requests.AppraisalRequestEntity;
import com.example.pandatribe.models.results.AppraisalResult;
import com.example.pandatribe.models.results.AppraisalResultEntity;
import com.example.pandatribe.models.results.CompressEntry;
import com.example.pandatribe.models.results.CompressResult;
import com.example.pandatribe.models.results.ReprocessEntry;
import com.example.pandatribe.models.results.ReprocessResult;
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
    public static final int MAX_APPRAISAL_LINES = 1000;
    private final MarketService marketService;
    private final EveTypesRepository eveTypesRepository;
    private final BlueprintQueryRepository blueprintQueryRepository;
    private final AppraisalDataRepository appraisalDataRepository;
    private final InvTypeMaterialRepository invTypeMaterialRepository;
    private final EveImageService eveImageService;
    private final EncodingUtil encodingUtil;
    private final EveDataConfig eveDataConfig;

    @Override
    public String generateAppraisalResult(AppraisalRequest appraisalRequest) {
        List<AppraisalRequestEntity> lines = appraisalRequest.getAppraisalRequestEntityList();
        if (lines == null || lines.isEmpty()) {
            throw new InvalidRequestException("Appraisal list cannot be empty");
        }
        if (lines.size() > MAX_APPRAISAL_LINES) {
            throw new InvalidRequestException("Appraisals are limited to " + MAX_APPRAISAL_LINES + " lines");
        }
        final Long locationId = eveDataConfig.resolveLocationId(appraisalRequest.getRegionId());
        final Integer regionId = eveDataConfig.resolveRegionId(appraisalRequest.getRegionId());

        // Pasted inventories often repeat the same item; price each distinct item once
        Map<String, Optional<AppraisalResultEntity>> pricedByName = new LinkedHashMap<>(); // keeps paste order
        List<AppraisalResultEntity> appraisalEntities = lines.stream()
                .filter(line -> line != null && line.getName() != null
                        && line.getQuantity() != null && line.getQuantity() > 0)
                .map(line -> pricedByName
                        .computeIfAbsent(line.getName(), name -> priceItem(name, regionId, locationId))
                        .map(priced -> priced.toBuilder().quantity(line.getQuantity()).build())
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
        List<String> missingItems = pricedByName.entrySet().stream()
                .filter(e -> e.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .toList();

        AppraisalResult appraisalResult =  AppraisalResult.builder()
                .appraisals(appraisalEntities)
                .missingItems(missingItems)
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

    /** Prices one item (quantity left unset); empty if the name isn't a known EVE type. */
    private Optional<AppraisalResultEntity> priceItem(String name, Integer regionId, Long locationId) {
        EveType eveType = eveTypesRepository.findEveTypeByTypeName(name).stream().findFirst().orElse(null);
        if (eveType == null) {
            LOGGER.warn("Eve item not found, skipping: '{}'", name);
            return Optional.empty();
        }
        Double volume = blueprintQueryRepository.getVolume(eveType.getTypeId());
        List<MarketOrderEntity> orders = marketService.getMarketOrders(eveType.getTypeId(), regionId);
        BigDecimal buyOrderPrice = marketService.getItemPriceByOrderTypeFromOrders("buy", orders, locationId);
        BigDecimal sellOrderPrice = marketService.getItemPriceByOrderTypeFromOrders("sell", orders, locationId);
        return Optional.of(AppraisalResultEntity.builder()
                .icon(eveImageService.generateIconLink(eveType.getTypeId(), eveType.getTypeName(), 32))
                .volume(Objects.nonNull(volume) ? volume : eveType.getVolume())
                .item(eveType.getTypeName())
                .buyOrdersCount(orders.stream().filter(o -> Boolean.TRUE.equals(o.getIsBuyOrder())).count())
                .buyOrderPrice(buyOrderPrice)
                .sellOrderPrice(sellOrderPrice)
                // ISK prices have 2 decimals; rounding to whole ISK skewed cheap items (minerals) by ~20%
                .splitPrice(buyOrderPrice.add(sellOrderPrice).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP))
                .sellOrdersCount(orders.stream().filter(o -> Boolean.FALSE.equals(o.getIsBuyOrder())).count())
                .build());
    }

    public AppraisalData getAppraisalResult(String id) {
        UUID uuid;
        try {
            uuid = encodingUtil.decompressUUID(id);
        } catch (IllegalArgumentException e) {
            throw new RequestNotFoundException(id, e); // a garbled link is just "not found"
        }
        return appraisalDataRepository.findById(uuid).orElseThrow(() -> new RequestNotFoundException(id));
    }

    @Override
    public ReprocessResult getReprocessResult(String id, Double efficiency) {
        AppraisalData appraisalData = getAppraisalResult(id);
        Long locationId = eveDataConfig.resolveLocationId(appraisalData.getMarket());
        Integer regionId = eveDataConfig.resolveRegionId(appraisalData.getMarket());

        // Aggregate yields across all items
        Map<Integer, Long> materialTotals = new HashMap<>();
        appraisalData.getAppraisalResult().getAppraisals().forEach(entity -> {
            if (entity == null) {
                return;
            }
            EveType type = eveTypesRepository.findEveTypeByTypeName(entity.getItem())
                    .stream().findFirst().orElse(null);
            if (type == null) {
                return;
            }

            int portionSize = type.getPortionSize() != null ? type.getPortionSize() : 1;
            long batches = (long) entity.getQuantity() / portionSize;
            if (batches == 0) {
                return;
            }

            invTypeMaterialRepository.findByIdTypeId(type.getTypeId()).forEach(mat -> {
                long yield = Math.round(batches * mat.getQuantity() * efficiency);
                materialTotals.merge(mat.getId().getMaterialTypeId(), yield, Long::sum);
            });
        });

        List<ReprocessEntry> entries = materialTotals.entrySet().stream()
                .map(e -> {
                    EveType matType = eveTypesRepository.findEveTypeByTypeId(e.getKey()).orElse(null);
                    if (matType == null) {
                        return null;
                    }
                    List<MarketOrderEntity> matOrders = marketService.getMarketOrders(e.getKey(), regionId);
                    BigDecimal buy  = marketService.getItemPriceByOrderTypeFromOrders("buy",  matOrders, locationId);
                    BigDecimal sell = marketService.getItemPriceByOrderTypeFromOrders("sell", matOrders, locationId);
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
        Long locationId = eveDataConfig.resolveLocationId(appraisalData.getMarket());
        Integer regionId = eveDataConfig.resolveRegionId(appraisalData.getMarket());

        List<CompressEntry> entries = appraisalData.getAppraisalResult().getAppraisals().stream()
                .filter(Objects::nonNull)
                .map(entity -> {
                    EveType type = eveTypesRepository.findEveTypeByTypeName(entity.getItem())
                            .stream().findFirst().orElse(null);
                    if (type == null) {
                        return null;
                    }

                    EveType compressed = eveTypesRepository
                            .findEveTypeByTypeName("Compressed " + type.getTypeName())
                            .stream().findFirst().orElse(null);
                    if (compressed == null) {
                        return null;
                    }

                    int portionSize = type.getPortionSize() != null ? type.getPortionSize() : 100;
                    long compressedQty = (long) entity.getQuantity() / portionSize;
                    if (compressedQty == 0) {
                        return null;
                    }
                    long remainder = (long) entity.getQuantity() % portionSize;

                    List<MarketOrderEntity> origPrices = marketService.getMarketOrders(type.getTypeId(), regionId);
                    List<MarketOrderEntity> compPrices  = marketService.getMarketOrders(compressed.getTypeId(), regionId);

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
                            .originalSellPrice(marketService.getItemPriceByOrderTypeFromOrders("sell", origPrices, locationId))
                            .originalBuyPrice(marketService.getItemPriceByOrderTypeFromOrders("buy",  origPrices, locationId))
                            .compressedSellPrice(marketService.getItemPriceByOrderTypeFromOrders("sell", compPrices, locationId))
                            .compressedBuyPrice(marketService.getItemPriceByOrderTypeFromOrders("buy",  compPrices, locationId))
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
}
