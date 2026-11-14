package com.example.pandatribe.repositories.interfaces;

import com.example.pandatribe.models.dbmodels.auth.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    Optional<UserSession> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
