package com.example.pandatribe.repositories.interfaces;

import com.example.pandatribe.models.authentication.OAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<OAuthToken, String> {
   Optional<OAuthToken> findOAuthTokenByCharacterId(Integer characterId);
}
