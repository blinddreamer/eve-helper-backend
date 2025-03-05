package com.example.pandatribe.services.contracts;

import com.example.pandatribe.models.requests.AppraisalRequest;
import com.example.pandatribe.models.results.AppraisalResult;

public interface AppraisalService {

    AppraisalResult generateAppraisalResult(AppraisalRequest appraisalRequest);
}
