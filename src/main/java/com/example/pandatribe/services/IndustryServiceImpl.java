package com.example.pandatribe.services;

import com.example.pandatribe.feign.contracts.EveInteractor;
import com.example.pandatribe.models.industry.SystemCostIndexes;
import com.example.pandatribe.services.contracts.IndustryService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class IndustryServiceImpl implements IndustryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndustryServiceImpl.class);
    private final EveInteractor eveInteractor;
    @Override
    @Cacheable(value = "cacheCostIndexes", cacheManager = "costIndexCacheManager")
    public List<SystemCostIndexes> getSystemCostIndexes() {
       List<SystemCostIndexes> costIndexes = eveInteractor.getSystemCostIndexes();
       LOGGER.info("Cost indexes received {}", !costIndexes.isEmpty());
      return costIndexes;
    }
}
