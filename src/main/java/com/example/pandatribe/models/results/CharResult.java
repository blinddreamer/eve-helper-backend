package com.example.pandatribe.models.results;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CharResult {
    private Integer charId;
    private String avatar;
    private String name;
}
