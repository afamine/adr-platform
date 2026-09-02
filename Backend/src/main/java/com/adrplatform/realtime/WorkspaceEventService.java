package com.adrplatform.realtime;

import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceEventService {
    private static final long STREAM_TIMEOUT_MS = 0L;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Map<UUID, CopyOnWriteArraySet<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter subscribe(User user) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        CopyOnWriteArraySet<SseEmitter> emitters = emittersByUser.computeIfAbsent(user.getId(), ignored -> new CopyOnWriteArraySet<>());
        emitters.add(emitter);
        emitter.onCompletion(() -> remove(user.getId(), emitter));
        emitter.onTimeout(() -> remove(user.getId(), emitter));
        emitter.onError(error -> remove(user.getId(), emitter));
        send(emitter, "connected", Map.of("workspaceId", user.getWorkspace().getId().toString()));
        return emitter;
    }

    public void publishToWorkspace(UUID workspaceId, String type, UUID adrId) {
        WorkspaceEvent event = WorkspaceEvent.of(type, workspaceId, adrId);
        Runnable dispatch = () -> userRepository.findAllByWorkspace_Id(workspaceId)
                .forEach(user -> sendToUser(user.getId(), event));
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { dispatch.run(); }
            });
        } else {
            dispatch.run();
        }
    }

    @Scheduled(fixedRate = 25000)
    public void sendHeartbeats() {
        emittersByUser.forEach((userId, emitters) -> emitters.forEach(emitter -> send(emitter, "ping", Map.of())));
    }

    private void sendToUser(UUID userId, WorkspaceEvent event) {
        CopyOnWriteArraySet<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters != null) emitters.forEach(emitter -> send(emitter, "workspace-event", event));
    }

    private void send(SseEmitter emitter, String name, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(name).data(objectMapper.writeValueAsString(payload)));
        } catch (IOException | IllegalStateException ex) {
            emitter.complete();
        }
    }

    private void remove(UUID userId, SseEmitter emitter) {
        CopyOnWriteArraySet<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) return;
        emitters.remove(emitter);
        if (emitters.isEmpty()) emittersByUser.remove(userId, emitters);
    }
}