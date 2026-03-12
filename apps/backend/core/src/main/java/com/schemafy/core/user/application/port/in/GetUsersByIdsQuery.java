package com.schemafy.core.user.application.port.in;

import java.util.Set;

public record GetUsersByIdsQuery(Set<String> userIds) {
}
