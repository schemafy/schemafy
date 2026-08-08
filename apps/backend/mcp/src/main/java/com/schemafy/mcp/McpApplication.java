package com.schemafy.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(basePackages = {
  "com.schemafy.mcp",
  "com.schemafy.core",
}, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.schemafy\\.core\\.user\\.application\\.service\\.(LoginUserService|SignUpUserService|SendSignUpEmailCodeService|VerifySignUpEmailService)"))
@ConfigurationPropertiesScan(basePackages = {
  "com.schemafy.mcp",
  "com.schemafy.core",
})
public class McpApplication {

  public static void main(String[] args) {
    SpringApplication.run(McpApplication.class, args);
  }

}
