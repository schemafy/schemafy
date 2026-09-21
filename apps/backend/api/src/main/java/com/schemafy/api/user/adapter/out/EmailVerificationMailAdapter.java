package com.schemafy.api.user.adapter.out;

import java.time.Instant;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.schemafy.api.user.config.AuthMailProperties;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.user.application.port.out.SendEmailVerificationPort;
import com.schemafy.core.user.domain.exception.UserErrorCode;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@Profile("!test")
public class EmailVerificationMailAdapter implements SendEmailVerificationPort {

  private final JavaMailSender mailSender;
  private final String from;

  public EmailVerificationMailAdapter(
      JavaMailSender mailSender,
      AuthMailProperties authMailProperties) {
    this.mailSender = mailSender;
    this.from = authMailProperties.getFrom();
  }

  @Override
  public Mono<Void> sendVerificationCode(String email, String code,
      Instant expiresAt) {
    return Mono.fromCallable(() -> message(email, code))
        .doOnNext(mailSender::send)
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorMap(error -> error instanceof MailException || error instanceof MessagingException,
            e -> {
              log.warn("Failed to send signup verification email. toDomain={}, from={}, cause={}: {}",
                  emailDomain(email), from, e.getClass().getSimpleName(), e.getMessage());
              return new DomainException(
                  UserErrorCode.EMAIL_DELIVERY_FAILED,
                  "Failed to send verification email");
            })
        .then();
  }

  private MimeMessage message(String email, String code) throws MessagingException {
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
    helper.setFrom(from);
    helper.setTo(email);
    helper.setSubject(EmailVerificationMailTemplate.SUBJECT);
    helper.setText(
        EmailVerificationMailTemplate.text(code),
        EmailVerificationMailTemplate.html(code));
    return message;
  }

  private String emailDomain(String email) {
    int atIndex = email == null ? -1 : email.indexOf('@');
    return atIndex >= 0 && atIndex + 1 < email.length()
        ? email.substring(atIndex + 1)
        : "unknown";
  }

}
