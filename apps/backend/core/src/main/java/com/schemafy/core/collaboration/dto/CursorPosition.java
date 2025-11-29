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

    private String userId;
    private String userName;
    private String userColor;
    private double x;
    private double y;
    private double viewportX;
    private double viewportY;
    private double zoom;
    private long timestamp;

}
