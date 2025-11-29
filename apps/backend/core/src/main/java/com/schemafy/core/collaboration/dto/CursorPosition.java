package com.schemafy.core.collaboration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPosition {

    private String userName;
    private double x;
    private double y;

    public CursorPosition withUserName(String userName) {
        return CursorPosition.builder()
                .userName(userName)
                .x(this.x)
                .y(this.y)
                .build();
    }

}
