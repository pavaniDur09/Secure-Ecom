package com.ecommerce.security.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Logs important security events. In this simplified version it just writes to the
 * console/log file with a clear format, matching the "Sample Audit Logs" section of
 * the project spec. In a real system you'd also save these rows to an `audit_logs`
 * table (shared across all tenants) so you can search/report on them later.
 */
@Service
@Slf4j
public class AuditService {

    public void logSuccess(String eventType, String email, String tenantId, String detail) {
        log.info("{} | INFO  | {} | {} | {} | {}",
                LocalDateTime.now(), eventType, email, tenantId, detail);
    }

    public void logWarning(String eventType, String email, String tenantId, String detail) {
        log.warn("{} | WARN  | {} | {} | {} | {}",
                LocalDateTime.now(), eventType, email, tenantId, detail);
    }

    public void logFailure(String eventType, String email, String tenantId, String detail) {
        log.error("{} | ERROR | {} | {} | {} | {}",
                LocalDateTime.now(), eventType, email, tenantId, detail);
    }
}
