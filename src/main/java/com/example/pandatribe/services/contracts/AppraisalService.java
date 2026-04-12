package com.example.pandatribe.services.contracts;

import com.example.pandatribe.models.dbmodels.appraisal.AppraisalData;
import com.example.pandatribe.models.requests.AppraisalRequest;
import com.example.pandatribe.models.results.CompressResult;
import com.example.pandatribe.models.results.ReprocessResult;

public interface AppraisalService {

    String generateAppraisalResult(AppraisalRequest appraisalRequest);
    AppraisalData getAppraisalResult(String id);
    ReprocessResult getReprocessResult(String uuid, Double efficiency);
    CompressResult getCompressResult(String uuid);
}
