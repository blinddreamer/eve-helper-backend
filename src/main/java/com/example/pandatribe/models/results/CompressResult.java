package com.example.pandatribe.models.results;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompressResult {
    private List<CompressEntry> items;
    private Double totalOriginalVolume;
    private Double totalCompressedVolume;
    private Double totalVolumeSaved;
}
