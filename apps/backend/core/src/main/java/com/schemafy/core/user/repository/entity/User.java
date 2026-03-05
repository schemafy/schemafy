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

/** Transitional legacy entity for read compatibility in core module.
 * <p>
 * User business write/read ownership is moving to domain user adapters.
 * Keep this entity for compatibility only until project/workspace tracks are migrated. */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table("users")
@Deprecated(forRemoval = false)
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

  public static User signUpOAuth(String email, String name) {
    Email validatedEmail = new Email(email);
    User newUser = new User(
        validatedEmail.address(),
        name,
        null,
        UserStatus.ACTIVE);
    newUser.setId(UlidGenerator.generate());
    return newUser;
  }

  public Mono<Boolean> matchesPassword(String rawPassword,
      PasswordEncoder passwordEncoder) {
    if (this.password == null) {
      return Mono.just(false);
    }
    return Mono.fromCallable(
        () -> passwordEncoder.matches(rawPassword, this.password))
        .subscribeOn(Schedulers.boundedElastic());
  }

}
