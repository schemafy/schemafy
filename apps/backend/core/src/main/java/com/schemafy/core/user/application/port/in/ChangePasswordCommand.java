package com.schemafy.core.user.application.port.in;

import com.schemafy.core.user.domain.UserPolicy;

public record ChangePasswordCommand(String currentPassword, String resetToken, String newPassword) {

  public ChangePasswordCommand {
    UserPolicy.validatePassword(newPassword);
  }

}
