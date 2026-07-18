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

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationMailAdapter")
class EmailVerificationMailAdapterTest {

  @Mock
  JavaMailSender mailSender;

  @Test
  @DisplayName("HTML과 텍스트 본문을 포함한 인증 메일을 발송한다")
  void sendVerificationCode_sendsMultipartEmail() throws Exception {
    MimeMessage mimeMessage = new JavaMailSenderImpl().createMimeMessage();
    given(mailSender.createMimeMessage()).willReturn(mimeMessage);
    EmailVerificationMailAdapter sut = new EmailVerificationMailAdapter(
        mailSender,
        "no-reply@schemafy.com");

    StepVerifier.create(sut.sendVerificationCode(
        "user@example.com",
        "123456",
        Instant.now().plusSeconds(60)))
        .verifyComplete();

    ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(
        MimeMessage.class);
    verify(mailSender).send(messageCaptor.capture());
    MimeMessage sentMessage = messageCaptor.getValue();

    assertThat(sentMessage.getSubject()).isEqualTo(
        EmailVerificationMailTemplate.SUBJECT);
    assertThat(sentMessage.getAllRecipients()[0].toString())
        .isEqualTo("user@example.com");
    assertThat(sentMessage.getFrom()[0].toString())
        .isEqualTo("no-reply@schemafy.com");
    assertThat(messageText(sentMessage))
        .contains("123456");
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
