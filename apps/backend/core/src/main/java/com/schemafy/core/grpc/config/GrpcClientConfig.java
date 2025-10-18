package com.schemafy.core.grpc.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import validation.ValidationServiceGrpc;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class GrpcClientConfig {

    @Value("${grpc.validation.host:localhost}")
    private String host;

    @Value("${grpc.validation.port:50051}")
    private int port;

    @Value("${grpc.validation.deadline-seconds:30}")
    private long deadlineSeconds;

    private ManagedChannel channel;

    @Bean
    public ManagedChannel validationChannel() {
        channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .enableRetry()
                .maxRetryAttempts(3)
                .build();

        log.info(
                "[GrpcClientConfig::validationChannel] gRPC channel created for validation service at {}:{}",
                host,
                port);
        return channel;
    }

    @Bean
    public ValidationServiceGrpc.ValidationServiceFutureStub validationFutureStub(
            ManagedChannel channel) {
        return ValidationServiceGrpc.newFutureStub(channel);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            try {
                log.info(
                        "[GrpcClientConfig::shutdown] Shutting down gRPC channel for validation service");
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.warn(
                        "[GrpcClientConfig::shutdown] Failed to shutdown gRPC channel gracefully",
                        e);
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
