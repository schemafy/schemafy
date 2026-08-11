package com.schemafy.core;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.schemafy.core.config.CoreTestConfiguration;

@SpringBootApplication
@Import(CoreTestConfiguration.class)
public class DomainTestApplication {
}
