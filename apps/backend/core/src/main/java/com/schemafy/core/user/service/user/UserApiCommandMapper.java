package com.schemafy.core.user.service.user;

import org.springframework.stereotype.Component;

import com.schemafy.core.user.service.dto.LoginCommand;
import com.schemafy.core.user.service.dto.OAuthLoginCommand;
import com.schemafy.core.user.service.dto.SignUpCommand;
import com.schemafy.domain.user.application.port.in.GetUserByIdQuery;
import com.schemafy.domain.user.application.port.in.LoginOrSignUpOAuthCommand;
import com.schemafy.domain.user.application.port.in.LoginUserCommand;
import com.schemafy.domain.user.application.port.in.SignUpUserCommand;

@Component
public class UserApiCommandMapper {

  public SignUpUserCommand toSignUpUserCommand(SignUpCommand command) {
    return new SignUpUserCommand(command.email(), command.name(), command.password());
  }

  public LoginUserCommand toLoginUserCommand(LoginCommand command) {
    return new LoginUserCommand(command.email(), command.password());
  }

  public GetUserByIdQuery toGetUserByIdQuery(String userId) {
    return new GetUserByIdQuery(userId);
  }

  public LoginOrSignUpOAuthCommand toLoginOrSignUpOAuthCommand(OAuthLoginCommand command) {
    return new LoginOrSignUpOAuthCommand(
        command.email(),
        command.name(),
        command.provider(),
        command.providerUserId());
  }

}
