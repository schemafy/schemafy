package com.schemafy.core.project.application.service;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.project.application.port.in.UpdateProjectShareLinkCommand;
import com.schemafy.core.project.application.port.out.ShareLinkPort;
import com.schemafy.core.project.domain.Project;
import com.schemafy.core.project.domain.ShareLink;
import com.schemafy.core.project.domain.exception.ProjectErrorCode;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

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

  @Mock
  private ProjectMutationGuard projectMutationGuard;

  @BeforeEach
  void setUp() {
    lenient().when(projectMutationGuard.<ShareLink>protectChildCreation(eq(PROJECT_ID), any()))
        .thenAnswer(invocation -> {
          Supplier<Mono<ShareLink>> action = invocation.getArgument(1);
          return Mono.defer(action);
        });
  }

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
    given(shareLinkPort.save(ArgumentMatchers.any(ShareLink.class)))
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
    assertThat(returned).extracting(ShareLink::getCode).containsOnly(stored.get().getCode());
    assertThat(stored.get().getCode()).matches("[0-9a-f]{32}");
    assertThat(reads.get()).isEqualTo(3);
  }

  @Test
  @DisplayName("같은 활성 상태 요청은 저장하지 않고 현재 링크를 반환한다")
  void sameActivationStateDoesNotSave() {
    ShareLink activeLink = ShareLink.create(
        "existing-link", PROJECT_ID, "0123456789abcdef0123456789abcdef");
    UpdateProjectShareLinkService sut = service();

    given(shareLinkHelper.findProjectById(PROJECT_ID)).willReturn(Mono.just(project()));
    given(shareLinkPort.findByProjectIdAndNotDeleted(PROJECT_ID))
        .willReturn(Mono.just(activeLink));

    ShareLink returned = sut.updateProjectShareLink(command(true)).block();

    assertThat(returned).isSameAs(activeLink);
    then(shareLinkPort).should().findByProjectIdAndNotDeleted(PROJECT_ID);
    then(shareLinkPort).shouldHaveNoMoreInteractions();
  }

  @Test
  @DisplayName("프로젝트 공유 잠금을 획득한 뒤에만 최초 링크를 조회하고 저장한다")
  void firstActivationWaitsForGuardBeforeReadingOrSaving() {
    UpdateProjectShareLinkService sut = service();
    Sinks.Empty<Void> lockAcquired = Sinks.empty();
    given(projectMutationGuard.<ShareLink>protectChildCreation(eq(PROJECT_ID), any()))
        .willAnswer(invocation -> {
          Supplier<Mono<ShareLink>> action = invocation.getArgument(1);
          return lockAcquired.asMono().then(Mono.defer(action));
        });
    given(shareLinkHelper.findProjectById(PROJECT_ID)).willReturn(Mono.just(project()));
    given(shareLinkPort.findByProjectIdAndNotDeleted(PROJECT_ID)).willReturn(Mono.empty());
    given(ulidGeneratorPort.generate()).willReturn("new-link-id");
    given(shareLinkPort.save(any(ShareLink.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(sut.updateProjectShareLink(command(true)))
        .then(() -> {
          then(shareLinkHelper).shouldHaveNoInteractions();
          then(shareLinkPort).shouldHaveNoInteractions();
          then(ulidGeneratorPort).shouldHaveNoInteractions();
          lockAcquired.tryEmitEmpty();
        })
        .assertNext(link -> {
          assertThat(link.getId()).isEqualTo("new-link-id");
          assertThat(link.isActive()).isTrue();
        })
        .verifyComplete();

    then(projectMutationGuard).should().protectChildCreation(eq(PROJECT_ID), any());
    then(shareLinkPort).should().save(any(ShareLink.class));
  }

  @Test
  @DisplayName("잠금 획득 시 프로젝트가 삭제됐으면 링크 조회와 저장을 수행하지 않는다")
  void deletedProjectDoesNotReadOrSaveShareLink() {
    UpdateProjectShareLinkService sut = service();
    given(projectMutationGuard.<ShareLink>protectChildCreation(eq(PROJECT_ID), any()))
        .willReturn(Mono.error(new DomainException(ProjectErrorCode.NOT_FOUND)));

    StepVerifier.create(sut.updateProjectShareLink(command(true)))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.NOT_FOUND))
        .verify();

    then(shareLinkHelper).shouldHaveNoInteractions();
    then(shareLinkPort).shouldHaveNoInteractions();
    then(ulidGeneratorPort).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("최초 생성 중복은 실패한 잠금 구간을 벗어난 뒤 새 잠금 구간에서 저장된 링크로 복구한다")
  void creationConflictRecoversInANewGuardedAttempt() {
    UpdateProjectShareLinkService sut = service();
    AtomicInteger attempts = new AtomicInteger();
    AtomicBoolean firstAttemptFailed = new AtomicBoolean();
    ShareLink winner = ShareLink.create("winner", PROJECT_ID,
        "0123456789abcdef0123456789abcdef");
    given(projectMutationGuard.<ShareLink>protectChildCreation(eq(PROJECT_ID), any()))
        .willAnswer(invocation -> {
          Supplier<Mono<ShareLink>> action = invocation.getArgument(1);
          int attempt = attempts.incrementAndGet();
          if (attempt == 1) {
            return Mono.defer(action).doOnError(error -> firstAttemptFailed.set(true));
          }
          assertThat(firstAttemptFailed).isTrue();
          return Mono.defer(action);
        });
    given(shareLinkHelper.findProjectById(PROJECT_ID)).willReturn(Mono.just(project()));
    given(shareLinkPort.findByProjectIdAndNotDeleted(PROJECT_ID))
        .willReturn(Mono.empty(), Mono.just(winner));
    given(ulidGeneratorPort.generate()).willReturn("loser");
    given(shareLinkPort.save(any(ShareLink.class)))
        .willReturn(Mono.error(new DataIntegrityViolationException("duplicate project_id")));

    StepVerifier.create(sut.updateProjectShareLink(command(true)))
        .expectNext(winner)
        .verifyComplete();

    then(projectMutationGuard).should(times(2)).protectChildCreation(eq(PROJECT_ID), any());
    then(shareLinkPort).should(times(2)).findByProjectIdAndNotDeleted(PROJECT_ID);
  }

  @Test
  @DisplayName("생성 오류 뒤 복구 조회가 비어 있으면 원래 DB 오류를 전달한다")
  void missingWinnerPreservesOriginalCreationError() {
    UpdateProjectShareLinkService sut = service();
    DataIntegrityViolationException original = new DataIntegrityViolationException("invalid insert");
    given(shareLinkHelper.findProjectById(PROJECT_ID)).willReturn(Mono.just(project()));
    given(shareLinkPort.findByProjectIdAndNotDeleted(PROJECT_ID)).willReturn(Mono.empty());
    given(ulidGeneratorPort.generate()).willReturn("new-link-id");
    given(shareLinkPort.save(any(ShareLink.class))).willReturn(Mono.error(original));

    StepVerifier.create(sut.updateProjectShareLink(command(true)))
        .expectErrorSatisfies(error -> assertThat(error).isSameAs(original))
        .verify();

    then(projectMutationGuard).should(times(2)).protectChildCreation(eq(PROJECT_ID), any());
  }

  @Test
  @DisplayName("기존 링크 상태 저장 오류는 생성 중복 복구로 숨기지 않는다")
  void stateUpdateErrorIsNotRecoveredAsCreationConflict() {
    UpdateProjectShareLinkService sut = service();
    ShareLink existing = ShareLink.create("existing", PROJECT_ID,
        "0123456789abcdef0123456789abcdef");
    existing.deactivate();
    DataIntegrityViolationException original = new DataIntegrityViolationException("update failed");
    given(shareLinkHelper.findProjectById(PROJECT_ID)).willReturn(Mono.just(project()));
    given(shareLinkPort.findByProjectIdAndNotDeleted(PROJECT_ID)).willReturn(Mono.just(existing));
    given(shareLinkPort.save(existing)).willReturn(Mono.error(original));

    StepVerifier.create(sut.updateProjectShareLink(command(true)))
        .expectErrorSatisfies(error -> assertThat(error).isSameAs(original))
        .verify();

    then(projectMutationGuard).should().protectChildCreation(eq(PROJECT_ID), any());
    then(shareLinkPort).should().findByProjectIdAndNotDeleted(PROJECT_ID);
    then(ulidGeneratorPort).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("생성 중복 이후 프로젝트가 삭제되면 복구 조회 없이 NOT_FOUND를 전달한다")
  void deletionBetweenCreationConflictAndRecoveryPreventsRecoveryRead() {
    UpdateProjectShareLinkService sut = service();
    AtomicInteger attempts = new AtomicInteger();
    given(projectMutationGuard.<ShareLink>protectChildCreation(eq(PROJECT_ID), any()))
        .willAnswer(invocation -> {
          if (attempts.incrementAndGet() == 2) {
            return Mono.error(new DomainException(ProjectErrorCode.NOT_FOUND));
          }
          Supplier<Mono<ShareLink>> action = invocation.getArgument(1);
          return Mono.defer(action);
        });
    given(shareLinkHelper.findProjectById(PROJECT_ID)).willReturn(Mono.just(project()));
    given(shareLinkPort.findByProjectIdAndNotDeleted(PROJECT_ID)).willReturn(Mono.empty());
    given(ulidGeneratorPort.generate()).willReturn("new-link-id");
    given(shareLinkPort.save(any(ShareLink.class)))
        .willReturn(Mono.error(new DataIntegrityViolationException("duplicate project_id")));

    StepVerifier.create(sut.updateProjectShareLink(command(true)))
        .expectErrorMatches(DomainException.hasErrorCode(ProjectErrorCode.NOT_FOUND))
        .verify();

    then(shareLinkPort).should().findByProjectIdAndNotDeleted(PROJECT_ID);
    then(shareLinkPort).should().save(any(ShareLink.class));
    then(shareLinkPort).shouldHaveNoMoreInteractions();
  }

  private UpdateProjectShareLinkService service() {
    return new UpdateProjectShareLinkService(ulidGeneratorPort, shareLinkHelper,
        shareLinkPort, projectMutationGuard);
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
