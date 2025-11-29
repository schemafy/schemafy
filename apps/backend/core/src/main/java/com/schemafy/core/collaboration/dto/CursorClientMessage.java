package com.schemafy.core.collaboration.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CursorClientMessage extends ClientMessage {

    private CursorPosition cursor;

}
