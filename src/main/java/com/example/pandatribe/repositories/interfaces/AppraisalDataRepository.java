package com.example.pandatribe.repositories.interfaces;

import com.example.pandatribe.models.AppraisalData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface AppraisalDataRepository extends JpaRepository<AppraisalData, UUID> {

    List<AppraisalData> findAppraisalDataByCreationDateBefore(Date creationDateBefore);
}
