package com.sushil.base_project.core.audit;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sushil.base_project.core.audit.internal.AuditLog;
import com.sushil.base_project.core.audit.internal.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String eventType, String subject, String ip, String userAgent, String details) {
        AuditLog log = new AuditLog();
        log.setEventType(eventType);
        log.setSubject(subject);
        log.setIp(ip);
        log.setUserAgent(userAgent);
        log.setDetails(details);
        log.setCreatedAt(Instant.now());
        repository.save(log);
    }
}
