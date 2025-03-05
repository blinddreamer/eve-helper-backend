package com.example.pandatribe.models.requests;

import lombok.Data;

import java.util.List;

@Data
public class AppraisalRequest {
    List<AppraisalRequestEntity> appraisalRequestEntityList;
    Integer regionId;
}
