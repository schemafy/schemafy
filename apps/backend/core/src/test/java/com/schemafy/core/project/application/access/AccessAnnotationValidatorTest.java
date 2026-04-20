package com.schemafy.core.project.application.access;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AccessAnnotationValidator")
class AccessAnnotationValidatorTest {

  @Test
  @DisplayName("정상적인 access annotation은 context startup을 통과한다")
  void validAccessAnnotations_passValidation() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ValidConfig.class)) {
      assertThat(context.getBean(ValidService.class)).isNotNull();
    }
  }

  @Test
  @DisplayName("프로젝트와 워크스페이스 access annotation이 함께 붙으면 실패한다")
  void dualAnnotations_failValidation() {
    assertThatThrownBy(() -> new AnnotationConfigApplicationContext(DualAnnotationConfig.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Only one access annotation can be declared on"
            + " DualAnnotatedService#invalid");
  }

  @Test
  @DisplayName("Mono/Flux가 아닌 반환 타입은 실패한다")
  void nonReactiveReturnType_failValidation() {
    assertThatThrownBy(() -> new AnnotationConfigApplicationContext(NonReactiveReturnConfig.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Access annotations are only supported on Mono/Flux methods:"
            + " NonReactiveService#invalid");
  }

  @Test
  @DisplayName("프로젝트 access accessor가 없으면 실패한다")
  void projectAccessorMismatch_failValidation() {
    assertThatThrownBy(() -> new AnnotationConfigApplicationContext(ProjectMismatchConfig.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Access annotation requires projectId accessor targetProjectId() on"
            + " ProjectRequest for ProjectMismatchService#invalid");
  }

  @Test
  @DisplayName("워크스페이스 access accessor가 없으면 실패한다")
  void workspaceAccessorMismatch_failValidation() {
    assertThatThrownBy(() -> new AnnotationConfigApplicationContext(WorkspaceMismatchConfig.class))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Access annotation requires workspaceId accessor targetWorkspaceId() on"
            + " WorkspaceRequest for WorkspaceMismatchService#invalid");
  }

  @Configuration
  static class ValidatorBeanConfig {

    @Bean
    AccessAnnotationValidator accessAnnotationValidator(
        org.springframework.context.ApplicationContext applicationContext) {
      return new AccessAnnotationValidator(applicationContext);
    }

  }

  @Configuration
  @org.springframework.context.annotation.Import(ValidatorBeanConfig.class)
  static class ValidConfig {

    @Bean
    ValidService validService() {
      return new ValidService();
    }

  }

  @Configuration
  @org.springframework.context.annotation.Import(ValidatorBeanConfig.class)
  static class DualAnnotationConfig {

    @Bean
    DualAnnotatedService dualAnnotatedService() {
      return new DualAnnotatedService();
    }

  }

  @Configuration
  @org.springframework.context.annotation.Import(ValidatorBeanConfig.class)
  static class NonReactiveReturnConfig {

    @Bean
    NonReactiveService nonReactiveService() {
      return new NonReactiveService();
    }

  }

  @Configuration
  @org.springframework.context.annotation.Import(ValidatorBeanConfig.class)
  static class ProjectMismatchConfig {

    @Bean
    ProjectMismatchService projectMismatchService() {
      return new ProjectMismatchService();
    }

  }

  @Configuration
  @org.springframework.context.annotation.Import(ValidatorBeanConfig.class)
  static class WorkspaceMismatchConfig {

    @Bean
    WorkspaceMismatchService workspaceMismatchService() {
      return new WorkspaceMismatchService();
    }

  }

  static class ValidService {

    @RequireProjectAccess
    public Mono<String> project(ProjectRequest request) {
      return Mono.just("ok");
    }

    @RequireWorkspaceAccess
    public Flux<String> workspace(WorkspaceRequest request) {
      return Flux.just("ok");
    }

  }

  static class DualAnnotatedService {

    @RequireProjectAccess
    @RequireWorkspaceAccess
    public Mono<String> invalid(ProjectRequest request) {
      return Mono.just("invalid");
    }

  }

  static class NonReactiveService {

    @RequireProjectAccess
    public String invalid(ProjectRequest request) {
      return "invalid";
    }

  }

  static class ProjectMismatchService {

    @RequireProjectAccess(projectId = "targetProjectId")
    public Mono<String> invalid(ProjectRequest request) {
      return Mono.just("invalid");
    }

  }

  static class WorkspaceMismatchService {

    @RequireWorkspaceAccess(workspaceId = "targetWorkspaceId")
    public Mono<String> invalid(WorkspaceRequest request) {
      return Mono.just("invalid");
    }

  }

  record ProjectRequest(String projectId, String requesterId) {
  }

  record WorkspaceRequest(String workspaceId, String requesterId) {
  }

}
