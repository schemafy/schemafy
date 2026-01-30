package com.schemafy.core.erd.controller.dto.response;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExportResponse {

  private String format;
  private String schemaName;
  private String ddl;
  private int tableCount;
  private Instant exportedAt;

  public static ExportResponse of(String schemaName, String ddl,
      int tableCount) {
    return ExportResponse.builder()
        .format("mysql_ddl")
        .schemaName(schemaName)
        .ddl(ddl)
        .tableCount(tableCount)
        .exportedAt(Instant.now())
        .build();
  }

}
