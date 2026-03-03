package com.schemafy.core.common.exception;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "problem")
public class ProblemProperties {

  private String typeBaseUri = "about:blank";

}
