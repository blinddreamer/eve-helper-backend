package com.example.pandatribe.repositories.interfaces;

import com.example.pandatribe.models.dbmodels.industry.BlueprintData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BlueprintDataRepository extends JpaRepository<BlueprintData, String> {

    List<BlueprintData> findByCreationDateBefore(LocalDate creationDate);
}
