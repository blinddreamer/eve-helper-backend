package com.example.pandatribe.repositories.interfaces;

import com.example.pandatribe.models.characters.CharacterData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CharacterDataRepository extends JpaRepository<CharacterData, Integer> {
}
