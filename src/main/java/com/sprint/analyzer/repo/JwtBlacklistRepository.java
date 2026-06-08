package com.sprint.analyzer.repo;

import com.sprint.analyzer.entity.JwtBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface JwtBlacklistRepository extends JpaRepository<JwtBlacklist, UUID> {

    boolean existsByToken(String token);

    @Modifying
    @Query("DELETE FROM JwtBlacklist j WHERE j.expiryDate <= :now")
    void deleteExpiredTokens(LocalDateTime now);
}