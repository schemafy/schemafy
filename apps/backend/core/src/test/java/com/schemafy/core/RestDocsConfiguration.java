package com.schemafy.core;

import org.springframework.boot.test.autoconfigure.restdocs.RestDocsWebTestClientConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentationConfigurer;

@Configuration
public class RestDocsConfiguration {

    @Bean
    public RestDocsWebTestClientConfigurationCustomizer restDocsWebTestClientConfigurationCustomizer() {
        return new RestDocsWebTestClientConfigurationCustomizer() {
            @Override
            public void customize(WebTestClientRestDocumentationConfigurer configurer) {
            }
        };
    }
}
