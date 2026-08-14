package com.schemafy.core.project.application.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.schemafy.core.project.application.port.in.AccessShareLinkQuery;
import com.schemafy.core.project.application.port.out.ProjectPort;
import com.schemafy.core.project.application.port.out.ShareLinkPort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ShareLink;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccessShareLinkService")
class AccessShareLinkServiceTest {

  private static final String PROJECT_ID = "project-id";
  private static final String SHARE_LINK_ID = "share-link-id";

  @Mock
  private ProjectPort projectPort;

  @Mock
  private ShareLinkPort shareLinkPort;

  private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
  private final Logger logger = (Logger) LoggerFactory.getLogger(AccessShareLinkService.class);

  @AfterEach
  void detachLogAppender() {
    logger.detachAppender(logAppender);
  }

  @Test
  @DisplayName("프로젝트 조회 성공 후에만 공유 링크 접근 이벤트를 한 번 기록한다")
  void logsAccessOnceAfterProjectLookupSucceeds() {
    given(shareLinkPort.findByIdAndNotDeleted(SHARE_LINK_ID))
        .willReturn(Mono.just(shareLink(true)));
    given(projectPort.findByIdAndNotDeleted(PROJECT_ID)).willAnswer(invocation -> {
      assertThat(accessLogs()).isEmpty();
      return Mono.just(project());
    });
    attachLogAppender();

    StepVerifier.create(service().accessShareLink(query(SHARE_LINK_ID)))
        .expectNextMatches(project -> PROJECT_ID.equals(project.getId()))
        .verifyComplete();

    assertThat(accessLogs()).singleElement().satisfies(event -> {
      assertThat(event.getMessage())
          .isEqualTo("event=share_link_access projectId={} shareLinkId={} ip={} userAgent={}");
      assertThat(event.getArgumentArray())
          .containsExactly(PROJECT_ID, SHARE_LINK_ID, "127.0.0.1", "test-agent");
      assertThat(event.getFormattedMessage())
          .doesNotContain("user-id", "actorId", "reason");
    });
  }

  @Test
  @DisplayName("비활성 또는 프로젝트 없는 공유 링크에는 접근 이벤트를 기록하지 않는다")
  void doesNotLogAccessForInactiveLinkOrMissingProject() {
    attachLogAppender();
    given(shareLinkPort.findByIdAndNotDeleted("inactive-link-id"))
        .willReturn(Mono.just(shareLink(false)));

    StepVerifier.create(service().accessShareLink(query("inactive-link-id")))
        .expectError()
        .verify();

    given(shareLinkPort.findByIdAndNotDeleted("missing-project-link-id"))
        .willReturn(Mono.just(shareLink(true)));
    given(projectPort.findByIdAndNotDeleted(PROJECT_ID)).willReturn(Mono.empty());

    StepVerifier.create(service().accessShareLink(query("missing-project-link-id")))
        .expectError()
        .verify();

    assertThat(accessLogs()).isEmpty();
  }

  private AccessShareLinkService service() {
    return new AccessShareLinkService(shareLinkPort, new ShareLinkHelper(projectPort));
  }

  private void attachLogAppender() {
    logAppender.start();
    logger.addAppender(logAppender);
  }

  private java.util.List<ILoggingEvent> accessLogs() {
    return logAppender.list.stream()
        .filter(event -> event.getMessage().startsWith("event=share_link_access"))
        .toList();
  }

  private AccessShareLinkQuery query(String shareLinkId) {
    return new AccessShareLinkQuery(shareLinkId, "user-id", "127.0.0.1", "test-agent");
  }

  private ShareLink shareLink(boolean active) {
    ShareLink shareLink = ShareLink.create(SHARE_LINK_ID, PROJECT_ID);
    if (active) {
      shareLink.activate();
    } else {
      shareLink.deactivate();
    }
    return shareLink;
  }

  private Project project() {
    return Project.create(PROJECT_ID, "workspace-id", 1, "Project", "Description");
  }

}
