package com.schemafy.core.project.adapter.out.persistence;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.relational.repository.Lock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("프로젝트 변경 저장소 잠금 메타데이터 테스트")
class RepositoryLockMetadataTest {

  @Test
  @DisplayName("프로젝트 저장소는 공유 잠금과 배타 잠금을 구분한다")
  void projectRepositoryDistinguishesSharedAndExclusiveLocks() {
    assertLockMode(ProjectRepository.class,
        "findWithSharedLockByIdAndDeletedAtIsNull",
        LockMode.PESSIMISTIC_READ);
    assertLockMode(ProjectRepository.class,
        "findWithExclusiveLockByIdAndDeletedAtIsNull",
        LockMode.PESSIMISTIC_WRITE);
  }

  @Test
  @DisplayName("워크스페이스 저장소는 공유 잠금과 배타 잠금을 구분한다")
  void workspaceRepositoryDistinguishesSharedAndExclusiveLocks() {
    assertLockMode(WorkspaceRepository.class,
        "findWithSharedLockByIdAndDeletedAtIsNull",
        LockMode.PESSIMISTIC_READ);
    assertLockMode(WorkspaceRepository.class,
        "findWithExclusiveLockByIdAndDeletedAtIsNull",
        LockMode.PESSIMISTIC_WRITE);
  }

  private void assertLockMode(Class<?> repositoryType, String methodName,
      LockMode expected) {
    Method method = Arrays.stream(repositoryType.getDeclaredMethods())
        .filter(candidate -> candidate.getName().equals(methodName))
        .findFirst()
        .orElseThrow(() -> new AssertionError(
            "잠금 저장소 메서드가 없습니다: " + methodName));

    Lock lock = method.getAnnotation(Lock.class);
    assertThat(lock)
        .as("%s의 @Lock", methodName)
        .isNotNull();
    assertThat(lock.value()).isEqualTo(expected);
  }

}
