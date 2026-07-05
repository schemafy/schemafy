package com.schemafy.api.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

import com.schemafy.api.common.resolver.ApiVersionArgumentResolver;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

  private final String[] openApiResourceLocations;

  public WebFluxConfig(
      @Value("${schemafy.openapi.resource-locations:classpath:/static/openapi/,file:build/api-spec/,file:apps/backend/api/build/api-spec/}") String[] openApiResourceLocations) {
    this.openApiResourceLocations = openApiResourceLocations;
  }

  @Override
  public void configureArgumentResolvers(
      @NonNull ArgumentResolverConfigurer configurer) {
    configurer.addCustomResolver(new ApiVersionArgumentResolver());
  }

  @Override
  public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/openapi/**")
        .addResourceLocations(openApiResourceLocations);
  }

}
