package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.entity.UserSession;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SessionService {

    private final UserSessionRepository sessionRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActiveSessions(Long userId) {
        return sessionRepository.findByUserIdAndIsActive(userId, true).stream()
                .map(this::toSessionData)
                .toList();
    }

    public void revokeSession(Long sessionId, String currentToken) {
        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (currentToken != null && currentToken.equals(session.getSessionToken())) {
            throw new BadRequestException("Cannot revoke current session. Use logout instead.");
        }

        deactivate(session);
        log.info("Session {} revoked", sessionId);
    }

    public int revokeAllOtherSessions(Long userId, String currentToken) {
        List<UserSession> activeSessions = sessionRepository.findByUserIdAndIsActive(userId, true);

        int revokedCount = 0;
        for (UserSession session : activeSessions) {
            if (currentToken != null && currentToken.equals(session.getSessionToken())) {
                continue;
            }
            deactivate(session);
            revokedCount++;
        }

        log.info("Revoked {} other sessions for user {}", revokedCount, userId);
        return revokedCount;
    }

    private void deactivate(UserSession session) {
        session.setIsActive(false);
        session.setLogoutAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    private Map<String, Object> toSessionData(UserSession session) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", session.getId());
        data.put("deviceInfo", session.getDeviceInfo());
        data.put("ipAddress", session.getIpAddress());
        data.put("location", session.getLocation());
        data.put("loginAt", session.getLoginAt());
        data.put("lastActivityAt", session.getLastActivityAt());
        data.put("isCurrentSession", false);
        return data;
    }
}
