package com.example.pandatribe.models.requests;

import lombok.Data;

import java.util.List;

@Data
public class AppraisalRequest {
    List<AppraisalRequestEntity> appraisalRequestEntityList;
    String regionId;
    Double pricePercentage;
    String transactionType;
    String comment;
}
