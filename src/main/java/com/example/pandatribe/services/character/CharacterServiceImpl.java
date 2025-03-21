package com.example.pandatribe.services.character;

import com.example.pandatribe.feign.contracts.EveInteractor;
import com.example.pandatribe.models.dbmodels.character.CharacterData;
import com.example.pandatribe.models.results.CharResult;
import com.example.pandatribe.repositories.interfaces.CharacterDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CharacterServiceImpl {
    private final EveInteractor eveInteractor;
    private final CharacterDataRepository characterDataRepository;

    public CharResult getCharacter(Integer characterId) {

        CharacterData csData = characterDataRepository.findById(characterId).orElse(null);
        if (csData == null) {
            throw new RuntimeException("character not found");
        }
        return CharResult.builder().charId(csData.getCharId()).name(csData.getCharName()).avatar(csData.getAvatar()).build();
    }
}
