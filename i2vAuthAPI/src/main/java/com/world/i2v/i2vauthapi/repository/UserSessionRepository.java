package com.world.i2v.i2vauthapi.repository;

import com.world.i2v.i2vauthapi.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionToken(String sessionToken);

    Optional<UserSession> findByRefreshToken(String refreshToken);

    // 🔥 ADD THIS METHOD
    List<UserSession> findByUserIdAndIsActive(Long userId, Boolean isActive);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false, s.logoutAt = CURRENT_TIMESTAMP " +
            "WHERE s.user.id = :userId AND s.isActive = true")
    void deactivateUserSessions(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < CURRENT_TIMESTAMP AND s.isActive = false")
    void deleteExpiredSessions();
}