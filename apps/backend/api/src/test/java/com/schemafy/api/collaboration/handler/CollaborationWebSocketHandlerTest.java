package com.schemafy.api.collaboration.handler;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketSession;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.api.collaboration.security.ProjectAccessValidator;
import com.schemafy.api.collaboration.service.CollaborationDirectMessageSender;
import com.schemafy.api.collaboration.service.CollaborationService;
import com.schemafy.api.collaboration.service.SessionRegistry;
import com.schemafy.api.collaboration.service.model.SessionEntry;
import com.schemafy.api.common.security.principal.AuthenticatedUser;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollaborationWebSocketHandler 단위 테스트")
class CollaborationWebSocketHandlerTest {

  @Mock
  private CollaborationDirectMessageSender directMessageSender;

  @Mock
  private CollaborationService collaborationService;

  @Mock
  private SessionRegistry sessionRegistry;

  @Mock
  private ProjectAccessValidator projectAccessValidator;

  @Mock
  private WebSocketSession session;

  @Mock
  private SessionEntry entry;

  @Test
  @DisplayName("인증된 연결은 SESSION_READY를 JOIN보다 먼저 전송한다")
  void handle_sends_session_ready_before_join() {
    CollaborationWebSocketHandler handler = new CollaborationWebSocketHandler(
        directMessageSender, collaborationService, sessionRegistry,
        projectAccessValidator);
    Authentication authentication = new UsernamePasswordAuthenticationToken(
        AuthenticatedUser.of("user-1", "tester"), null);
    HandshakeInfo handshakeInfo = new HandshakeInfo(
        URI.create("ws://localhost/ws/collaboration?projectId=project-1"),
        HttpHeaders.EMPTY, Mono.just(authentication), null);

    given(session.getHandshakeInfo()).willReturn(handshakeInfo);
    given(session.getId()).willReturn("session-1");
    given(projectAccessValidator.canAccess("project-1", "user-1"))
        .willReturn(Mono.just(true));
    given(sessionRegistry.addSession(eq("project-1"), eq("session-1"),
        eq(session), any()))
        .willReturn(entry);
    given(directMessageSender.sendSessionReady(entry, "session-1"))
        .willReturn(Mono.empty());
    given(collaborationService.notifyJoin("project-1", "session-1",
        "user-1", "tester"))
        .willReturn(Mono.empty());
    given(session.receive()).willReturn(Flux.never());
    given(entry.outboundFlux()).willReturn(Flux.never());
    given(session.send(any())).willReturn(Mono.never());
    given(session.close(any())).willReturn(Mono.empty());
    given(session.closeStatus()).willReturn(Mono.never());
    given(entry.sampledCursorFlux()).willReturn(Flux.never());
    given(collaborationService.removeSession("project-1", "session-1"))
        .willReturn(Mono.empty());

    Disposable subscription = handler.handle(session).subscribe();

    InOrder inOrder = inOrder(directMessageSender, collaborationService);
    inOrder.verify(directMessageSender).sendSessionReady(entry,
        "session-1");
    inOrder.verify(collaborationService).notifyJoin("project-1",
        "session-1", "user-1", "tester");

    subscription.dispose();

    verify(sessionRegistry).addSession(eq("project-1"), eq("session-1"),
        eq(session), any());
  }

}
