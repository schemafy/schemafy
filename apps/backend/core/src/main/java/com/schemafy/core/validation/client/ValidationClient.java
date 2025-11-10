package com.schemafy.core.validation.client;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.schemafy.core.common.exception.BusinessException;
import com.schemafy.core.common.exception.ErrorCode;
import com.schemafy.core.validation.exception.GrpcConnectionException;
import com.schemafy.core.validation.exception.ValidationFailedException;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import validation.Validation.AddColumnToConstraintRequest;
import validation.Validation.AddColumnToIndexRequest;
import validation.Validation.AddColumnToRelationshipRequest;
import validation.Validation.ChangeColumnNameRequest;
import validation.Validation.ChangeColumnPositionRequest;
import validation.Validation.ChangeColumnTypeRequest;
import validation.Validation.ChangeConstraintNameRequest;
import validation.Validation.ChangeIndexNameRequest;
import validation.Validation.ChangeRelationshipCardinalityRequest;
import validation.Validation.ChangeRelationshipNameRequest;
import validation.Validation.ChangeSchemaNameRequest;
import validation.Validation.ChangeTableNameRequest;
import validation.Validation.CreateColumnRequest;
import validation.Validation.CreateConstraintRequest;
import validation.Validation.CreateIndexRequest;
import validation.Validation.CreateRelationshipRequest;
import validation.Validation.CreateSchemaRequest;
import validation.Validation.CreateTableRequest;
import validation.Validation.Database;
import validation.Validation.DeleteColumnRequest;
import validation.Validation.DeleteConstraintRequest;
import validation.Validation.DeleteIndexRequest;
import validation.Validation.DeleteRelationshipRequest;
import validation.Validation.DeleteSchemaRequest;
import validation.Validation.DeleteTableRequest;
import validation.Validation.RemoveColumnFromConstraintRequest;
import validation.Validation.RemoveColumnFromIndexRequest;
import validation.Validation.RemoveColumnFromRelationshipRequest;
import validation.Validation.ValidateFailure;
import validation.Validation.ValidateResult;
import validation.ValidationServiceGrpc;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationClient {

    private final ValidationServiceGrpc.ValidationServiceFutureStub baseFutureStub;

    @Value("${grpc.validation.deadline-seconds:30}")
    private long deadlineSeconds;

    public Mono<Database> validateDatabase(Database database) {
        return executeValidation(
                () -> withDeadline().validateDatabase(database),
                "validateDatabase");
    }

    public Mono<Database> createColumn(CreateColumnRequest request) {
        return executeValidation(() -> withDeadline().createColumn(request),
                "createColumn");
    }

    public Mono<Database> deleteColumn(DeleteColumnRequest request) {
        return executeValidation(() -> withDeadline().deleteColumn(request),
                "deleteColumn");
    }

    public Mono<Database> changeColumnName(
            ChangeColumnNameRequest request) {
        return executeValidation(() -> withDeadline().changeColumnName(request),
                "changeColumnName");
    }

    public Mono<Database> changeColumnType(
            ChangeColumnTypeRequest request) {
        return executeValidation(() -> withDeadline().changeColumnType(request),
                "changeColumnType");
    }

    public Mono<Database> changeColumnPosition(
            ChangeColumnPositionRequest request) {
        return executeValidation(
                () -> withDeadline().changeColumnPosition(request),
                "changeColumnPosition");
    }

    public Mono<Database> createConstraint(
            CreateConstraintRequest request) {
        return executeValidation(() -> withDeadline().createConstraint(request),
                "createConstraint");
    }

    public Mono<Database> deleteConstraint(
            DeleteConstraintRequest request) {
        return executeValidation(() -> withDeadline().deleteConstraint(request),
                "deleteConstraint");
    }

    public Mono<Database> changeConstraintName(
            ChangeConstraintNameRequest request) {
        return executeValidation(
                () -> withDeadline().changeConstraintName(request),
                "changeConstraintName");
    }

    public Mono<Database> addColumnToConstraint(
            AddColumnToConstraintRequest request) {
        return executeValidation(
                () -> withDeadline().addColumnToConstraint(request),
                "addColumnToConstraint");
    }

    public Mono<Database> removeColumnFromConstraint(
            RemoveColumnFromConstraintRequest request) {
        return executeValidation(
                () -> withDeadline().removeColumnFromConstraint(request),
                "removeColumnFromConstraint");
    }

    public Mono<Database> createIndex(CreateIndexRequest request) {
        return executeValidation(() -> withDeadline().createIndex(request),
                "createIndex");
    }

    public Mono<Database> deleteIndex(DeleteIndexRequest request) {
        return executeValidation(() -> withDeadline().deleteIndex(request),
                "deleteIndex");
    }

    public Mono<Database> changeIndexName(
            ChangeIndexNameRequest request) {
        return executeValidation(() -> withDeadline().changeIndexName(request),
                "changeIndexName");
    }

    public Mono<Database> addColumnToIndex(
            AddColumnToIndexRequest request) {
        return executeValidation(() -> withDeadline().addColumnToIndex(request),
                "addColumnToIndex");
    }

    public Mono<Database> removeColumnFromIndex(
            RemoveColumnFromIndexRequest request) {
        return executeValidation(
                () -> withDeadline().removeColumnFromIndex(request),
                "removeColumnFromIndex");
    }

    public Mono<Database> createRelationship(
            CreateRelationshipRequest request) {
        return executeValidation(
                () -> withDeadline().createRelationship(request),
                "createRelationship");
    }

    public Mono<Database> deleteRelationship(
            DeleteRelationshipRequest request) {
        return executeValidation(
                () -> withDeadline().deleteRelationship(request),
                "deleteRelationship");
    }

    public Mono<Database> changeRelationshipName(
            ChangeRelationshipNameRequest request) {
        return executeValidation(
                () -> withDeadline().changeRelationshipName(request),
                "changeRelationshipName");
    }

    public Mono<Database> changeRelationshipCardinality(
            ChangeRelationshipCardinalityRequest request) {
        return executeValidation(
                () -> withDeadline().changeRelationshipCardinality(request),
                "changeRelationshipCardinality");
    }

    public Mono<Database> addColumnToRelationship(
            AddColumnToRelationshipRequest request) {
        return executeValidation(
                () -> withDeadline().addColumnToRelationship(request),
                "addColumnToRelationship");
    }

    public Mono<Database> removeColumnFromRelationship(
            RemoveColumnFromRelationshipRequest request) {
        return executeValidation(
                () -> withDeadline().removeColumnFromRelationship(request),
                "removeColumnFromRelationship");
    }

    public Mono<Database> createSchema(CreateSchemaRequest request) {
        return executeValidation(() -> withDeadline().createSchema(request),
                "createSchema");
    }

    public Mono<Database> deleteSchema(DeleteSchemaRequest request) {
        return executeValidation(() -> withDeadline().deleteSchema(request),
                "deleteSchema");
    }

    public Mono<Database> changeSchemaName(
            ChangeSchemaNameRequest request) {
        return executeValidation(() -> withDeadline().changeSchemaName(request),
                "changeSchemaName");
    }

    public Mono<Database> createTable(CreateTableRequest request) {
        return executeValidation(() -> withDeadline().createTable(request),
                "createTable");
    }

    public Mono<Database> deleteTable(DeleteTableRequest request) {
        return executeValidation(() -> withDeadline().deleteTable(request),
                "deleteTable");
    }

    public Mono<Database> changeTableName(
            ChangeTableNameRequest request) {
        return executeValidation(() -> withDeadline().changeTableName(request),
                "changeTableName");
    }

    private Mono<Database> executeValidation(ValidationCall call,
            String operationName) {
        return Mono.fromFuture(toCompletableFuture(call.execute()))
                .map(result -> {
                    if (result.hasFailure()) {
                        ValidateFailure failure = result.getFailure();
                        List<ValidationFailedException.ValidationError> errors = failure
                                .getErrorsList().stream()
                                .map(error -> new ValidationFailedException.ValidationError(
                                        error.getCode(),
                                        error.getMessage()))
                                .toList();
                        throw new ValidationFailedException(errors);
                    }
                    return result.getSuccess().getDatabase();
                })
                .doOnSubscribe(s -> log.debug(
                        "[ValidationClient::executeValidation] Executing validation: {}",
                        operationName))
                .doOnError(error -> log.error(
                        "[ValidationClient::executeValidation] Validation error in {}: {}",
                        operationName, error.getMessage()))
                .onErrorMap(this::mapGrpcException)
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static <T> CompletableFuture<T> toCompletableFuture(
            ListenableFuture<T> listenableFuture) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        Futures.addCallback(listenableFuture, new FutureCallback<>() {

            @Override
            public void onSuccess(T result) {
                completableFuture.complete(result);
            }

            @Override
            public void onFailure(Throwable t) {
                completableFuture.completeExceptionally(t);
            }

        }, MoreExecutors.directExecutor());
        return completableFuture;
    }

    private ValidationServiceGrpc.ValidationServiceFutureStub withDeadline() {
        return baseFutureStub.withDeadlineAfter(deadlineSeconds,
                TimeUnit.SECONDS);
    }

    private Throwable mapGrpcException(Throwable throwable) {
        if (throwable instanceof ValidationFailedException) {
            return throwable;
        }

        if (throwable instanceof StatusRuntimeException statusException) {
            Status status = statusException.getStatus();
            log.warn(
                    "[ValidationClient::mapGrpcException] gRPC call failed with status: {} - {}",
                    status.getCode(),
                    status.getDescription());

            return switch (status.getCode()) {
            case UNAVAILABLE, DEADLINE_EXCEEDED -> new GrpcConnectionException(
                    throwable);
            default -> new BusinessException(
                    ErrorCode.VALIDATION_ERROR);
            };
        }

        log.error(
                "[ValidationClient::mapGrpcException] Unexpected error during validation",
                throwable);
        return new BusinessException(
                ErrorCode.VALIDATION_ERROR);
    }

    @FunctionalInterface
    private interface ValidationCall {

        ListenableFuture<ValidateResult> execute();

    }

}
