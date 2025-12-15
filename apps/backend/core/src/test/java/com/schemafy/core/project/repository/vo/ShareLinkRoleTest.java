package com.schemafy.core.project.repository.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ShareLinkRole 테스트")
class ShareLinkRoleTest {

    @Test
    @DisplayName("VIEWER ShareLinkRole을 ProjectRole.VIEWER로 변환")
    void toProjectRole_Viewer_Success() {
        ShareLinkRole shareLinkRole = ShareLinkRole.VIEWER;

        ProjectRole projectRole = shareLinkRole.toProjectRole();

        assertThat(projectRole).isEqualTo(ProjectRole.VIEWER);
    }

    @Test
    @DisplayName("COMMENTER ShareLinkRole을 ProjectRole.COMMENTER로 변환")
    void toProjectRole_Commenter_Success() {
        ShareLinkRole shareLinkRole = ShareLinkRole.COMMENTER;

        ProjectRole projectRole = shareLinkRole.toProjectRole();

        assertThat(projectRole).isEqualTo(ProjectRole.COMMENTER);
    }

    @Test
    @DisplayName("EDITOR ShareLinkRole을 ProjectRole.EDITOR로 변환")
    void toProjectRole_Editor_Success() {
        ShareLinkRole shareLinkRole = ShareLinkRole.EDITOR;

        ProjectRole projectRole = shareLinkRole.toProjectRole();

        assertThat(projectRole).isEqualTo(ProjectRole.EDITOR);
    }

}
