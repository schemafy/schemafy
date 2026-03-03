package com.schemafy.domain;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.schemafy.domain.config.PasswordHashTestConfiguration;

@SpringBootApplication
@Import(PasswordHashTestConfiguration.class)
public class DomainTestApplication {

}
