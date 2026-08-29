package com.schemafy.api.user.adapter.out;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmailVerificationMailTemplate")
class EmailVerificationMailTemplateTest {

  @Test
  @DisplayName("브랜드 톤의 HTML 인증 메일을 렌더링한다")
  void html_rendersVerificationEmail() {
    String html = EmailVerificationMailTemplate.html("123456");

    assertThat(html)
        .contains("Schemafy")
        .contains("Verify your email")
        .contains("123456")
        .contains("This code expires in <strong>1 minute</strong>")
        .contains("background:#ffffff")
        .contains("color:#141414")
        .doesNotContain("%s");
  }

  @Test
  @DisplayName("HTML을 지원하지 않는 메일 클라이언트를 위한 텍스트 본문을 렌더링한다")
  void text_rendersFallbackBody() {
    String text = EmailVerificationMailTemplate.text("123456");

    assertThat(text)
        .contains("Schemafy email verification")
        .contains("123456")
        .contains("This code expires in 1 minute");
  }

}
