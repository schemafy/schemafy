package com.schemafy.core.common.config;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.MySqlDialect;

@Configuration
@EnableR2dbcAuditing
public class R2dbcConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Object> converters = new ArrayList<>();
        converters.add(new InstantToLocalDateTimeConverter());
        converters.add(new LocalDateTimeToInstantConverter());

        return R2dbcCustomConversions.of(MySqlDialect.INSTANCE, converters);
    }

    @WritingConverter
    static class InstantToLocalDateTimeConverter
            implements Converter<Instant, LocalDateTime> {

        @Override
        public LocalDateTime convert(Instant source) {
            return LocalDateTime.ofInstant(source, ZoneOffset.UTC);
        }

    }

    @ReadingConverter
    static class LocalDateTimeToInstantConverter
            implements Converter<LocalDateTime, Instant> {

        @Override
        public Instant convert(LocalDateTime source) {
            return source.toInstant(ZoneOffset.UTC);
        }

    }

}
