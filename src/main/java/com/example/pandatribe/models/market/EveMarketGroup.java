package com.example.pandatribe.models.market;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "\"invMarketGroups\"")
public class EveMarketGroup {

    @Id
    @Column(name = "\"marketGroupID\"")
    private Integer marketGroupId;
    @Column(name = "\"parentGroupID\"")
    private Integer parentGroupId;
    @Column(name = "\"marketGroupName\"")
    private String marketGroupName;
    @Column(name = "\"description\"")
    private String description;
    @Column(name = "\"iconID\"")
    private Integer iconId;
    @Column(name = "\"hasTypes\"")
    private Boolean hasTypes;
}
