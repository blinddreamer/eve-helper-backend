package com.example.pandatribe.controllers;

import com.example.pandatribe.models.industry.blueprints.PiMat;
import com.example.pandatribe.services.contracts.PiDataService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/")
public class EvePiController {

    private final PiDataService piDataService;

    @GetMapping("pi")
    public ResponseEntity<?> generatePi(){
     // Note: Market price fetching is already cached via cacheItemMarketPrice
     // The PI calculation itself is fast and doesn't need caching
     List<PiMat> result = piDataService.generatePi();
     return ResponseEntity.ok(result);
    }
}
