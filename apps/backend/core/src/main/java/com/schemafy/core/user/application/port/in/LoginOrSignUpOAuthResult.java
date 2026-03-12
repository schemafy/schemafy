package com.schemafy.core.user.application.port.in;

import com.schemafy.core.user.domain.User;

public record LoginOrSignUpOAuthResult(User user, boolean newUser) {
}
