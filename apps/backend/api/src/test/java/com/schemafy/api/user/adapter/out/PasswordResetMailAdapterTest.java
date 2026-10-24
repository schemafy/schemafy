package com.schemafy.api.user.adapter.out;

import java.time.Instant;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.api.common.config.AppProperties;
import com.schemafy.api.user.config.AuthMailProperties;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetMailAdapter")
class PasswordResetMailAdapterTest {

  @Mock
  JavaMailSender mailSender;

  @Test
  @DisplayName("HTML과 텍스트 본문을 포함한 비밀번호 재설정 메일을 발송한다")
  void sendPasswordResetLink_sendsMultipartEmail() throws Exception {
    MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
    given(mailSender.createMimeMessage()).willReturn(mimeMessage);
    AuthMailProperties authMailProperties = new AuthMailProperties();
    authMailProperties.setFrom("no-reply@schemafy.com");
    AppProperties appProperties = new AppProperties();
    appProperties.setFrontendUrl("http://localhost:3001");
    PasswordResetMailAdapter sut = new PasswordResetMailAdapter(
        mailSender, authMailProperties, appProperties);

    StepVerifier.create(sut.sendPasswordResetLink(
        "user@example.com", "user-id.raw-token", Instant.now().plusSeconds(600)))
        .verifyComplete();

    ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(mailSender).send(messageCaptor.capture());
    MimeMessage sentMessage = messageCaptor.getValue();

    assertThat(sentMessage.getSubject()).isEqualTo(PasswordResetMailTemplate.SUBJECT);
    assertThat(sentMessage.getAllRecipients()[0].toString()).isEqualTo("user@example.com");
    assertThat(sentMessage.getFrom()[0].toString()).isEqualTo("no-reply@schemafy.com");
    assertThat(sentMessage.getContent()).isInstanceOf(Multipart.class);
    assertThat(messageText(sentMessage))
        .contains("http://localhost:3001/reset-password#resetToken=user-id.raw-token");
  }

  private String messageText(MimeMessage message) throws Exception {
    return contentText(message.getContent());
  }

  private String contentText(Object content) throws Exception {
    if (content instanceof Multipart multipart) {
      StringBuilder builder = new StringBuilder();
      for (int index = 0; index < multipart.getCount(); index++) {
        BodyPart bodyPart = multipart.getBodyPart(index);
        builder.append(contentText(bodyPart.getContent())).append('\n');
      }
      return builder.toString();
    }
    return String.valueOf(content);
  }

}
