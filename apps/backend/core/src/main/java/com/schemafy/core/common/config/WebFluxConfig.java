package com.schemafy.core.common.config;

import com.schemafy.core.common.resolver.ApiVersionArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void configureArgumentResolvers(@NonNull ArgumentResolverConfigurer configurer) {
        configurer.addCustomResolver(new ApiVersionArgumentResolver());
    }
}
