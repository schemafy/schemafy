package com.schemafy.core.project.controller.dto.response;

import com.schemafy.core.project.repository.entity.Project;
import com.schemafy.core.project.repository.vo.ProjectSettings;
import com.schemafy.core.project.repository.vo.ShareLinkRole;

public record ShareLinkAccessResponse(

        String projectId,

        String projectName,

        String description,

        ProjectSettings settings,

        String grantedRole,

        boolean canEdit,

        boolean canComment) {

    public static ShareLinkAccessResponse of(Project project,
            ShareLinkRole role) {
        return new ShareLinkAccessResponse(project.getId(), project.getName(),
                project.getDescription(), project.getSettingsAsVo(),
                role.getValue(), role.canEdit(), role.canComment());
    }

}
