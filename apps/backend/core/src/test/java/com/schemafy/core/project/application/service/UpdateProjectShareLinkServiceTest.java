package com.schemafy.core.project.application.service;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.dao.DataIntegrityViolationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.project.application.port.in.UpdateProjectShareLinkCommand;
import com.schemafy.core.project.application.port.out.ShareLinkPort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ShareLink;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateProjectShareLinkService")
class UpdateProjectShareLinkServiceTest {

  private static final String PROJECT_ID = "project-id";
  private static final String REQUESTER_ID = "requester-id";

  @Mock
  private UlidGeneratorPort ulidGeneratorPort;

  @Mock
  private ShareLinkHelper shareLinkHelper;

  @Mock
  private ShareLinkPort shareLinkPort;

  @Test
  @DisplayName("동시 최초 ON에서 중복 생성은 저장된 같은 링크로 복구한다")
  void concurrentFirstActivationRecoversDuplicateCreate() {
    CountDownLatch bothObservedAbsent = new CountDownLatch(2);
    AtomicInteger reads = new AtomicInteger();
    AtomicReference<ShareLink> stored = new AtomicReference<>();
    UpdateProjectShareLinkService sut = service();
    UpdateProjectShareLinkCommand command = command(true);

    given(shareLinkHelper.findProjectById(PROJECT_ID)).willReturn(Mono.just(project()));
    given(shareLinkPort.findByProjectIdAndNotDeleted(PROJECT_ID))
        .willAnswer(invocation -> Mono.defer(() -> {
          if (reads.incrementAndGet() <= 2) {
            return waitForBothAbsent(bothObservedAbsent);
          }
          return Mono.just(stored.get());
        }));
    given(ulidGeneratorPort.generate()).willReturn("link-id-1", "link-id-2");
    given(shareLinkPort.save(org.mockito.ArgumentMatchers.any(ShareLink.class)))
        .willAnswer(invocation -> {
          ShareLink candidate = invocation.getArgument(0);
          return stored.compareAndSet(null, candidate)
              ? Mono.just(candidate)
              : Mono.error(new DataIntegrityViolationException("duplicate project_id"));
        });

    var returned = Flux.merge(
        Mono.defer(() -> sut.updateProjectShareLink(command))
            .subscribeOn(Schedulers.parallel()),
        Mono.defer(() -> sut.updateProjectShareLink(command))
            .subscribeOn(Schedulers.parallel()))
        .collectList()
        .block(Duration.ofSeconds(10));

    assertThat(returned).hasSize(2);
    assertThat(returned).extracting(ShareLink::getId).containsOnly(stored.get().getId());
    assertThat(reads.get()).isEqualTo(3);
  }

  @Test
  @DisplayName("같은 활성 상태 요청은 저장하지 않고 현재 링크를 반환한다")
  void sameActivationStateDoesNotSave() {
    ShareLink activeLink = ShareLink.create("existing-link", PROJECT_ID);
    UpdateProjectShareLinkService sut = service();

    given(shareLinkHelper.findProjectById(PROJECT_ID)).willReturn(Mono.just(project()));
    given(shareLinkPort.findByProjectIdAndNotDeleted(PROJECT_ID))
        .willReturn(Mono.just(activeLink));

    ShareLink returned = sut.updateProjectShareLink(command(true)).block();

    assertThat(returned).isSameAs(activeLink);
    then(shareLinkPort).should().findByProjectIdAndNotDeleted(PROJECT_ID);
    then(shareLinkPort).shouldHaveNoMoreInteractions();
  }

  private UpdateProjectShareLinkService service() {
    return new UpdateProjectShareLinkService(ulidGeneratorPort, shareLinkHelper,
        shareLinkPort);
  }

  private Mono<ShareLink> waitForBothAbsent(CountDownLatch latch) {
    return Mono.fromCallable(() -> {
      latch.countDown();
      if (!latch.await(2, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Both requests did not observe an absent link");
      }
      return (ShareLink) null;
    }).subscribeOn(Schedulers.boundedElastic());
  }

  private UpdateProjectShareLinkCommand command(boolean isActive) {
    return new UpdateProjectShareLinkCommand(PROJECT_ID, isActive, REQUESTER_ID);
  }

  private Project project() {
    return Project.create(PROJECT_ID, "workspace-id", 1, "Project", "Description");
  }

}
