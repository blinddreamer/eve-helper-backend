package com.example.pandatribe.repositories.interfaces;

import com.example.pandatribe.models.market.EveMarketGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EveMarketGroupsRepository extends JpaRepository<EveMarketGroup, Integer> {
}
