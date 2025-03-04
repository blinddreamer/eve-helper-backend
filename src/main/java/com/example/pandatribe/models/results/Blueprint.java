package com.example.pandatribe.models.results;

import lombok.Builder;
import lombok.Data;
import lombok.With;

@Data
@Builder
@With
public class Blueprint {
    private Integer bpId;
    private String blueprint;
    private Integer activity;
    private Integer complexity;
}
