package com.example.pandatribe.repositories.interfaces;

import com.example.pandatribe.models.BlueprintData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface BlueprintDataRepository extends JpaRepository<BlueprintData, String> {

    List<BlueprintData> findByCreationDateBefore(Date creationDate);
}
