package com.example.pandatribe.services;

import com.example.pandatribe.models.industry.blueprints.EveType;
import com.example.pandatribe.models.industry.blueprints.PiDependency;
import com.example.pandatribe.models.industry.blueprints.PiMat;
import com.example.pandatribe.models.market.ItemPrice;
import com.example.pandatribe.repositories.EveCustomRepositoryImpl;
import com.example.pandatribe.repositories.interfaces.EveTypesRepository;
import com.example.pandatribe.services.contracts.MarketService;
import com.example.pandatribe.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.example.pandatribe.utils.Constant.*;

@Service
@RequiredArgsConstructor
public class PiServiceImpl {
    private final EveCustomRepositoryImpl eveCustomRepositoryImpl;
    private final EveTypesRepository eveTypesRepository;
    private final MarketService marketService;
    private final Helper helper;
    private final HashMap<Integer, List<String>> planets = new HashMap<>(Map.ofEntries(
            //Barren
            Map.entry(1, Arrays.asList("Aqueous Liquids", "Base Metals", "Carbon Compounds", "Micro Organisms", "Noble Metals")),
            //Gas
            Map.entry(2, Arrays.asList("Aqueous Liquids", "Base Metals", "Ionic Solutions", "Noble Gas", "Reactive Gas")),
            //Ice
            Map.entry(3, Arrays.asList("Aqueous Liquids", "Micro Organisms", "Planktic Colonies", "Complex Organisms", "Carbon Compounds")),
            //Lava
            Map.entry(4, Arrays.asList("Base Metals", "Felsic Magma", "Noble Metals", "Non-CS Crystals", "Suspended Plasma")),
            //Oceanic
            Map.entry(5, Arrays.asList("Aqueous Liquids", "Carbon Compounds", "Complex Organisms", "Micro Organisms", "Planktic Colonies")),
            //Plasma
            Map.entry(6, Arrays.asList("Base Metals", "Noble Metals", "Non-CS Crystals", "Suspended Plasma", "Felsic Magma")),
            //Storm
            Map.entry(7, Arrays.asList("Aqueous Liquids", "Ionic Solutions", "Noble Gas", "Reactive Gas", "Suspended Plasma")),
            //Temperate
            Map.entry(8, Arrays.asList("Aqueous Liquids", "Autotrophs", "Carbon Compounds", "Complex Organisms", "Micro Organisms"))));

    public List<PiMat> generatePi(){
        List<Integer> materials = eveCustomRepositoryImpl.getRawMaterials();
       return materials.stream().map(id-> {
            EveType eveType = eveTypesRepository.findEveTypeByTypeId(id).orElse(null);
            if (eveType == null) {
                return null;
            }
            Integer schematicID = eveCustomRepositoryImpl.getSchematicId(eveType.getTypeName());
            List<ItemPrice> itemPriceList = marketService.getItemMarketPrice(eveType.getTypeId(), DEFAULT_REGION_ID, SELL_ORDER_TYPE
            );
            List<PiDependency> piDependencies;
            Integer type = validateType(eveType.getGroupId());
            if(type==0){
               piDependencies = planets.entrySet().stream().filter(list-> list.getValue().contains(eveType.getTypeName())).map(list->
                       PiDependency.builder().typeID(list.getKey()).build()
                        ).toList();
             } else{
             piDependencies = eveCustomRepositoryImpl.getPiDependency(schematicID);
                }
            return PiMat.builder()
                    .id(eveType.getTypeId())
                    .quantity(type!=0 ? piDependencies.stream().filter(d-> !d.getIsInput()).findFirst()
                            .map(PiDependency::getQuantity).orElse(null) : null)
                    .price(marketService.getItemSellOrderPrice(DEFAULT_LOCATION_ID, itemPriceList))
                    .name(eveType.getTypeName())
                    .icon(helper.generateIconLink(eveType.getTypeId(), 32))
                    .type(type)
                    .dependencies(type != 0 ? piDependencies.stream().filter(PiDependency::getIsInput).toList() : piDependencies)
                    .build();
        })
               .filter(Objects::nonNull)
               .sorted(Comparator.comparing(PiMat::getName))
               .sorted(Comparator.comparing(PiMat::getType))
               .toList();
    }

    private Integer validateType(Integer groupId){
        return switch (groupId) {
            case 1034 -> 2;
            case 1042 -> 1;
            case 1040 -> 3;
            case 1041 -> 4;
            default -> 0;
        };
    }
}
