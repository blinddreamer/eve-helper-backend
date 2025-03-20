package com.example.pandatribe.models.characters;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CharacterLoginInfo {
    @JsonProperty("CharacterID")
    private Integer characterId;

    @JsonProperty("CharacterName")
    private String name;
}
