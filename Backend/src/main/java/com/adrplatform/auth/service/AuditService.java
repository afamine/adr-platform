package com.adrplatform.auth.service;

import com.adrplatform.auth.domain.AuditEvent;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.domain.Workspace;
import com.adrplatform.auth.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public void record(User user,
                       Workspace workspace,
                       String action,
                       String entityType,
                       UUID entityId,
                       String oldValueJson,
                       String newValueJson) {
        AuditEvent event = AuditEvent.builder()
                .workspace(workspace)
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValueJson(oldValueJson)
                .newValueJson(newValueJson)
                .build();
        auditEventRepository.save(event);
        log.debug("Audit event saved: action={}, entityType={}, entityId={}", action, entityType, entityId);
    }
}
