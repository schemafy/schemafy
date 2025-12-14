package com.schemafy.core.collaboration.service.model;

import java.time.Duration;

import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.schemafy.core.collaboration.dto.CursorPosition;
import com.schemafy.core.collaboration.security.WebSocketAuthInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class SessionEntry {

    private static final Duration CURSOR_SAMPLE_INTERVAL = Duration
            .ofMillis(50);

    private final WebSocketSession session;
    private final WebSocketAuthInfo authInfo;

    private final Sinks.Many<String> outboundSink;
    private final Flux<WebSocketMessage> outboundFlux;

    private final Sinks.Many<CursorPosition> cursorSink;

    public SessionEntry(WebSocketSession session, WebSocketAuthInfo authInfo) {
        this.session = session;
        this.authInfo = authInfo;

        this.outboundSink = Sinks.many()
                .unicast()
                .onBackpressureBuffer();
        this.outboundFlux = outboundSink.asFlux()
                .map(session::textMessage);

        this.cursorSink = Sinks.many()
                .unicast()
                .onBackpressureBuffer();
    }

    public WebSocketSession session() {
        return session;
    }

    public WebSocketAuthInfo authInfo() {
        return authInfo;
    }

    public Flux<WebSocketMessage> outboundFlux() {
        return outboundFlux;
    }

    public Flux<CursorPosition> sampledCursorFlux() {
        return cursorSink.asFlux()
                .sample(CURSOR_SAMPLE_INTERVAL);
    }

    public void pushCursor(CursorPosition cursor) {
        cursorSink.tryEmitNext(cursor);
    }

    public Sinks.EmitResult send(String message) {
        return outboundSink.tryEmitNext(message);
    }

    public void complete() {
        outboundSink.tryEmitComplete();
        cursorSink.tryEmitComplete();
    }

    public boolean isOpen() { return session.isOpen(); }

}
