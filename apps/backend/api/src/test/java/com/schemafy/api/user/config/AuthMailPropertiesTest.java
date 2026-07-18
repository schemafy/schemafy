package com.schemafy.api.user.config;

import java.util.function.Consumer;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("인증 메일 설정")
class AuthMailPropertiesTest {

  @Test
  @DisplayName("프로필 설정이 누락되면 인증 메일을 기본 활성화한다")
  void mailEnabledByDefault() {
    AuthMailProperties properties = new AuthMailProperties();

    assertThat(properties.isEnabled()).isTrue();
  }

  @Test
  @DisplayName("local 프로필은 SMTP 설정이 없으면 인증 메일을 비활성화한다")
  void localProfileDisablesMailByDefault() {
    runWithProfile("local", properties -> assertThat(properties.isEnabled()).isFalse());
  }

  @Test
  @DisplayName("server 프로필은 SMTP 설정이 없으면 인증 메일을 활성화한다")
  void serverProfileEnablesMailByDefault() {
    runWithProfile("server", properties -> assertThat(properties.isEnabled()).isTrue());
  }

  private void runWithProfile(
      String profile,
      Consumer<AuthMailProperties> assertion) {
    new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withUserConfiguration(TestConfiguration.class)
        .withPropertyValues("spring.profiles.active=" + profile)
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertion.accept(context.getBean(AuthMailProperties.class));
        });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(AuthMailProperties.class)
  static class TestConfiguration {
  }

}
