package com.example.pandatribe.models.dbmodels.industry;

import com.example.pandatribe.convertors.JsonToBlueprintData;
import com.example.pandatribe.models.results.BlueprintResult;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Entity
@RequiredArgsConstructor
@Builder
@With
@Table(name = "search_requests")
@AllArgsConstructor
public class BlueprintData {
    @Id
    @Column(name="id")
    private String id;

    @Column(name = "blueprint_data",columnDefinition = "jsonb")
    @Convert(converter = JsonToBlueprintData.class)
    private List<BlueprintResult> blueprintResult;

    @Column(name="creation_date")
    private LocalDate creationDate;
}
