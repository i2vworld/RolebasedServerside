package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.entity.AuditLog;
import com.world.i2v.i2vauthapi.entity.User;
import com.world.i2v.i2vauthapi.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    public void logAction(User user, String action, String entityType, Long entityId, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);

        auditLogRepository.save(auditLog);
        log.debug("Audit log saved: {} - {} - {}", action, entityType, entityId);
    }

    @Async
    public void logDetailedAction(User user, String action, String entityType, Long entityId,
                                  Map<String, Object> oldValue, Map<String, Object> newValue) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);

        auditLogRepository.save(auditLog);
        log.debug("Detailed audit log saved: {} - {} - {}", action, entityType, entityId);
    }
}