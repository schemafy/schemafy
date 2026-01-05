package com.schemafy.core.user.repository.entity;

import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.schemafy.core.common.type.BaseEntity;
import com.schemafy.core.ulid.generator.UlidGenerator;
import com.schemafy.core.user.repository.vo.Email;
import com.schemafy.core.user.repository.vo.UserInfo;
import com.schemafy.core.user.repository.vo.UserStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("users")
public class User extends BaseEntity {

  private String email;

  private String name;

  private String password;

  private UserStatus status;

  public static Mono<User> signUp(UserInfo userInfo,
      PasswordEncoder passwordEncoder) {
    return Mono.fromCallable(() -> {
      Email email = new Email(userInfo.email());
      String encodedPassword = passwordEncoder
          .encode(userInfo.password());

      User newUser = new User(
          email.address(),
          userInfo.name(),
          encodedPassword,
          UserStatus.ACTIVE);
      newUser.setId(UlidGenerator.generate());

      return newUser;
    }).subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<Boolean> matchesPassword(String rawPassword,
      PasswordEncoder passwordEncoder) {
    return Mono.fromCallable(
        () -> passwordEncoder.matches(rawPassword, this.password))
        .subscribeOn(Schedulers.boundedElastic());
  }

}
