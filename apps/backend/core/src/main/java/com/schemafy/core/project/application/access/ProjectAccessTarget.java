package com.schemafy.core.project.application.access;

record ProjectAccessTarget(
    ProjectAccessResourceType type,
    String accessorName) {
}
