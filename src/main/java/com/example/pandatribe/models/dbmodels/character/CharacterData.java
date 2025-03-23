package com.example.pandatribe.models.dbmodels.character;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "character_info")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CharacterData {
    @Id
    @Column(name = "char_id")
    private int charId;

    @Column(name = "char_name")
    private String charName;

    @Column(name = "char_avatar")
    private String avatar;
}
