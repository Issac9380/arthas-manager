package com.arthasmanager.arthas.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry for all active {@link ArthasSession} objects.
 *
 * <p>A scheduled task evicts sessions that have been idle longer than
 * {@code arthas.session-timeout} minutes.
 */
@Slf4j
@Component
public class ArthasSessionManager {

    @Value("${arthas.session-timeout:30}")
    private int sessionTimeoutMinutes;

    private final ConcurrentHashMap<String, ArthasSession> sessions = new ConcurrentHashMap<>();

    public void put(ArthasSession session) {
        sessions.put(session.getSessionId(), session);
        log.info("Arthas session registered: {} → {}:{} pid={}",
                session.getSessionId(), session.getPodName(), session.getContainerName(), session.getPid());
    }

    public Optional<ArthasSession> get(String sessionId) {
        ArthasSession session = sessions.get(sessionId);
        if (session != null) {
            session.setLastUsedAt(Instant.now());
        }
        return Optional.ofNullable(session);
    }

    public void remove(String sessionId) {
        ArthasSession session = sessions.remove(sessionId);
        if (session != null) {
            session.close();
            log.info("Arthas session closed: {}", sessionId);
        }
    }

    public Collection<ArthasSession> all() {
        return sessions.values();
    }

    /** Evict idle sessions every 5 minutes. */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void evictIdleSessions() {
        Instant threshold = Instant.now().minus(sessionTimeoutMinutes, ChronoUnit.MINUTES);
        sessions.entrySet().removeIf(entry -> {
            ArthasSession s = entry.getValue();
            Instant last = s.getLastUsedAt() != null ? s.getLastUsedAt() : s.getCreatedAt();
            if (last.isBefore(threshold)) {
                s.close();
                log.info("Evicted idle Arthas session: {}", s.getSessionId());
                return true;
            }
            return false;
        });
    }
}
