package com.schemafy.core.common.config;

import com.schemafy.core.common.resolver.ApiVersionArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.http.codec.protobuf.ProtobufEncoder;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void configureArgumentResolvers(
            @NonNull ArgumentResolverConfigurer configurer) {
        configurer.addCustomResolver(new ApiVersionArgumentResolver());
    }

    @Override
    public void configureHttpMessageCodecs(
            @NonNull ServerCodecConfigurer configurer) {
        configurer.customCodecs().register(new ProtobufDecoder());
        configurer.customCodecs().register(new ProtobufEncoder());
    }

}
