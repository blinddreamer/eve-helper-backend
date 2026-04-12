package com.example.pandatribe.controllers;

import com.example.pandatribe.logging.JsonLogger;
import com.example.pandatribe.models.dbmodels.appraisal.AppraisalData;
import com.example.pandatribe.models.requests.AppraisalRequest;
import com.example.pandatribe.models.results.CompressResult;
import com.example.pandatribe.models.results.ReprocessResult;
import com.example.pandatribe.services.contracts.AppraisalService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/appraisal")
public class  EveAppraisalController {
    private static final Logger LOGGER = LoggerFactory.getLogger(EveAppraisalController.class);
    private final JsonLogger jsonLogger;
    private final AppraisalService appraisalService;

    @PostMapping()
    public ResponseEntity<String> getAppraisalPrices(@RequestBody AppraisalRequest request){
        String requestId = UUID.randomUUID().toString();
        LOGGER.info("REQUEST for appraisal with id {} received: ",requestId);
        jsonLogger.println(request);
        String appraisalResult = appraisalService.generateAppraisalResult(request);
        LOGGER.info("RESPONSE for appraisal with id {} ready - {}: ",requestId, appraisalResult);
        //jsonLogger.println(appraisalResult);
        return ResponseEntity.ok(appraisalResult);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppraisalData> getAppraisalPrices(@PathVariable String id){
        LOGGER.info("REQUEST for appraisal with id {} received: ", id);
        AppraisalData appraisalResult = appraisalService.getAppraisalResult(id);
        LOGGER.info("RESPONSE for appraisal with id {} ready: ", id);
        jsonLogger.println(appraisalResult);
        return ResponseEntity.ok(appraisalResult);
    }

    @GetMapping("/{id}/reprocess")
    public ResponseEntity<ReprocessResult> getReprocessResult(
            @PathVariable String id,
            @RequestParam(defaultValue = "0.875") Double efficiency) {
        LOGGER.info("REQUEST for reprocess on appraisal {} efficiency={}", id, efficiency);
        return ResponseEntity.ok(appraisalService.getReprocessResult(id, efficiency));
    }

    @GetMapping("/{id}/compress")
    public ResponseEntity<CompressResult> getCompressResult(@PathVariable String id) {
        LOGGER.info("REQUEST for compress on appraisal {}", id);
        return ResponseEntity.ok(appraisalService.getCompressResult(id));
    }

}
