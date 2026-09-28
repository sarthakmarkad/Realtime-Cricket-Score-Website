package com.cricket.scores.web;

import com.cricket.scores.sse.ScoreBroadcastService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Live score stream. Clients subscribe once and receive an "update" event for
 * every simulated delivery and every match status change.
 */
@RestController
public class StreamController {

    private final ScoreBroadcastService broadcaster;

    public StreamController(ScoreBroadcastService broadcaster) {
        this.broadcaster = broadcaster;
    }

    @GetMapping(value = "/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return broadcaster.register();
    }
}
