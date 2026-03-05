package com.schemafy.domain.user.application.port.in;

import com.schemafy.domain.user.domain.User;

public record LoginOrSignUpOAuthResult(User user, boolean newUser) {
}
