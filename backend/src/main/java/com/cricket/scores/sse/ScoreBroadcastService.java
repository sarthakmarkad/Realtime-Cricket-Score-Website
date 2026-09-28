package com.cricket.scores.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-Sent Events hub. Every connected dashboard client gets a push for each
 * simulated delivery plus heartbeats that keep proxies from closing idle streams.
 */
@Component
public class ScoreBroadcastService {

    private static final Logger log = LoggerFactory.getLogger(ScoreBroadcastService.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    public ScoreBroadcastService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"ok\":true}", MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        log.debug("SSE client connected (total {})", emitters.size());
        return emitter;
    }

    public void broadcast(String eventName, Object payload) {
        if (emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(objectMapper.writeValueAsString(payload), MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    @Scheduled(fixedRate = 15000)
    public void heartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }
}
