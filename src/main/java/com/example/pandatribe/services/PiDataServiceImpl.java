package com.example.pandatribe.services;

import com.example.pandatribe.models.industry.blueprints.EveType;
import com.example.pandatribe.models.industry.blueprints.PiDependency;
import com.example.pandatribe.models.industry.blueprints.PiMat;
import com.example.pandatribe.models.dbmodels.market.MarketOrderEntity;
import com.example.pandatribe.repositories.PlanetaryInteractionRepository;
import com.example.pandatribe.repositories.interfaces.EveTypesRepository;
import com.example.pandatribe.services.contracts.MarketService;
import com.example.pandatribe.services.contracts.PiDataService;
import com.example.pandatribe.utils.EveImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.pandatribe.utils.Constant.*;

@Service
@RequiredArgsConstructor
public class PiDataServiceImpl implements PiDataService {
    private final PlanetaryInteractionRepository planetaryInteractionRepository;
    private final EveTypesRepository eveTypesRepository;
    private final MarketService marketService;
    private final EveImageService eveImageService;
    private final HashMap<Integer, String> planetNames = new HashMap<>(Map.ofEntries(
            //Barren
            Map.entry(1, "Barren"),
            //Gas
            Map.entry(2, "Gas"),
            //Ice
            Map.entry(3, "Ice"),
            //Lava
            Map.entry(4, "Lava"),
            //Oceanic
            Map.entry(5, "Oceanic"),
            //Plasma
            Map.entry(6, "Plasma"),
            //Storm
            Map.entry(7, "Storm"),
            //Temperate
            Map.entry(8,"Temperate")));

    private final HashMap<Integer, List<String>> planetsMaterials = new HashMap<>(Map.ofEntries(
            //Barren
            Map.entry(1, Arrays.asList("Aqueous Liquids", "Base Metals", "Carbon Compounds", "Microorganisms", "Noble Metals")),
            //Gas
            Map.entry(2, Arrays.asList("Aqueous Liquids", "Base Metals", "Ionic Solutions", "Noble Gas", "Reactive Gas")),
            //Ice
            Map.entry(3, Arrays.asList("Aqueous Liquids", "Heavy Metals", "Microorganisms", "Noble Gas", "Planktic Colonies")),
            //Lava
            Map.entry(4, Arrays.asList("Base Metals", "Felsic Magma", "Heavy Metals", "Non-CS Crystals", "Suspended Plasma")),
            //Oceanic
            Map.entry(5, Arrays.asList("Aqueous Liquids", "Carbon Compounds", "Complex Organisms", "Microorganisms", "Planktic Colonies")),
            //Plasma
            Map.entry(6, Arrays.asList("Base Metals", "Heavy Metals", "Noble Metals", "Non-CS Crystals", "Suspended Plasma")),
            //Storm
            Map.entry(7, Arrays.asList("Aqueous Liquids", "Base Metals", "Ionic Solutions", "Noble Gas", "Suspended Plasma")),
            //Temperate
            Map.entry(8, Arrays.asList("Aqueous Liquids", "Autotrophs", "Carbon Compounds", "Complex Organisms", "Microorganisms"))));

    @Override
    public List<PiMat> generatePi(){
        // Fetch all PI materials
        List<Integer> materials = planetaryInteractionRepository.getRawMaterials();

        // Bulk fetch all schematic IDs in one query
        Map<Integer, Integer> schematicIdMap = planetaryInteractionRepository.getSchematicIdsBulk(materials);

        // Bulk fetch all cycle times in one query
        List<Integer> schematicIds = new ArrayList<>(schematicIdMap.values());
        Map<Integer, Integer> cycleTimeMap = planetaryInteractionRepository.getCycleTimesBulk(schematicIds);

        // Bulk fetch all PI dependencies in one query
        Map<Integer, List<PiDependency>> dependenciesMap = planetaryInteractionRepository.getPiDependenciesBulk(schematicIds);

        // Bulk fetch all EveType rows in one query instead of one query per material
        Map<Integer, EveType> eveTypesMap = eveTypesRepository.findAllById(materials).stream()
                .collect(Collectors.toMap(EveType::getTypeId, Function.identity()));

        List<PiMat> result = new ArrayList<>(materials.stream().map(id -> {
                   EveType eveType = eveTypesMap.get(id);
                   if (eveType == null) {
                       return null;
                   }

                   // Get pre-fetched data from maps instead of individual queries
                   Integer schematicID = schematicIdMap.get(eveType.getTypeId());
                   Integer cycleTime = schematicID != null ? cycleTimeMap.get(schematicID) : null;

                   // Read directly from DB — the PI warmup scheduler keeps this fresh every 15 min.
                   // Using getMarketOrders() here caused lock contention with the scheduler during ESI refresh.
                   List<MarketOrderEntity> orders = marketService.getMarketOrdersFromDb(eveType.getTypeId(), DEFAULT_REGION_ID);

                   List<PiDependency> piDependencies;
                   Integer type = validateType(eveType.getGroupId());
                   if (type == 1) {
                       piDependencies = planetsMaterials.entrySet().stream()
                               .filter(list -> list.getValue().contains(eveType.getTypeName()))
                               .map(list -> PiDependency.builder().typeID(list.getKey()).build())
                               .toList();
                   } else {
                       // Get pre-fetched dependencies from map
                       piDependencies = schematicID != null ?
                               dependenciesMap.getOrDefault(schematicID, new ArrayList<>()) :
                               new ArrayList<>();
                   }

                   return PiMat.builder()
                           .id(eveType.getTypeId())
                           .quantity(type > 1 ? piDependencies.stream().filter(d -> !d.getIsInput()).findFirst()
                                   .map(PiDependency::getQuantity).orElse(null) : null)
                           .price(marketService.getItemPriceByOrderTypeFromOrders(SELL_ORDER_TYPE, orders, DEFAULT_LOCATION_ID.longValue()))
                           .name(eveType.getTypeName())
                           .icon(eveImageService.generateIconLink(eveType.getTypeId(), 32))
                           .type(type)
                           .cycleTime(cycleTime)
                           .dependencies(type > 1 ? piDependencies.stream().filter(PiDependency::getIsInput).toList() : piDependencies)
                           .build();
               })
               .filter(Objects::nonNull)
               .sorted(Comparator.comparing(PiMat::getName))
               .sorted(Comparator.comparing(PiMat::getType))
               .toList());

       result.addAll(planetNames.entrySet().stream().map(list->
               PiMat.builder().id(list.getKey()).icon(String.format("/assets/%s.png", list.getValue().toLowerCase())).name(list.getValue()).type(validateType(0)).build()
       ).toList());

       return result;
    }

    private Integer validateType(Integer groupId){
        return switch (groupId) {
            case 1034 -> 3;
            case 1042 -> 2;
            case 1040 -> 4;
            case 1041 -> 5;
            case 0 -> 0;
            default -> 1;
        };
    }
}
