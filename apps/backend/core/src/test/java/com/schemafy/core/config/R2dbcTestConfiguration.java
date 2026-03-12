package com.schemafy.core.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

import io.r2dbc.spi.ConnectionFactory;

@TestConfiguration
@EnableR2dbcAuditing
public class R2dbcTestConfiguration {

  @Bean
  public ConnectionFactoryInitializer connectionFactoryInitializer(ConnectionFactory connectionFactory) {
    var initializer = new ConnectionFactoryInitializer();
    initializer.setConnectionFactory(connectionFactory);

    var populator = new CompositeDatabasePopulator();
    var resolver = new PathMatchingResourcePatternResolver();

    try {
      var resources = resolver.getResources("classpath:ddl/h2/*.sql");
      for (var resource : resources) {
        populator.addPopulators(new ResourceDatabasePopulator(resource));
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to load DDL scripts", e);
    }

    initializer.setDatabasePopulator(populator);
    return initializer;
  }

}
