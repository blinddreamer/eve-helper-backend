package com.example.pandatribe.controllers;

import com.example.pandatribe.models.industry.blueprints.EveType;
import com.example.pandatribe.models.industry.blueprints.PiMat;
import com.example.pandatribe.services.PiServiceImpl;
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

    private final PiServiceImpl piServiceImpl;

    @GetMapping("pi")
    public ResponseEntity<?> generatePi(){
     List<PiMat> result = piServiceImpl.generatePi();
     return ResponseEntity.ok(result);
    }
}
