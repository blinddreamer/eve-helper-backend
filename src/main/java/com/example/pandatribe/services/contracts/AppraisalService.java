package com.example.pandatribe.services.contracts;

import com.example.pandatribe.models.AppraisalData;
import com.example.pandatribe.models.requests.AppraisalRequest;

public interface AppraisalService {

    String generateAppraisalResult(AppraisalRequest appraisalRequest);
    AppraisalData getAppraisalResult(String id);
}
