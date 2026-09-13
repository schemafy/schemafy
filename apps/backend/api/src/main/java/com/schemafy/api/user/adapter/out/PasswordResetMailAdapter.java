package com.schemafy.api.user.adapter.out;

import java.time.Instant;

import jakarta.mail.internet.MimeMessage;

import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.schemafy.api.common.config.AppProperties;
import com.schemafy.api.user.config.AuthMailProperties;
import com.schemafy.core.user.application.port.out.SendPasswordResetEmailPort;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@Profile("!test")
public class PasswordResetMailAdapter implements SendPasswordResetEmailPort {

  private final JavaMailSender mailSender;
  private final String from;
  private final String frontendUrl;

  public PasswordResetMailAdapter(JavaMailSender mailSender, AuthMailProperties properties,
      AppProperties appProperties) {
    this.mailSender = mailSender;
    this.from = properties.getFrom();
    this.frontendUrl = appProperties.getFrontendUrl();
  }

  @Override
  public Mono<Void> sendPasswordResetLink(String email, String resetToken, Instant expiresAt) {
    return Mono.fromCallable(() -> {
      String link = frontendUrl.replaceFirst("/+$", "") + "/reset-password#resetToken=" + resetToken;
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(from);
      helper.setTo(email);
      helper.setSubject(PasswordResetMailTemplate.SUBJECT);
      helper.setText(PasswordResetMailTemplate.text(link), PasswordResetMailTemplate.html(link));
      return message;
    }).doOnNext(mailSender::send).subscribeOn(Schedulers.boundedElastic()).then();
  }

}
