package com.schemafy.core.erd.operation;

import java.util.function.Function;
import java.util.function.UnaryOperator;

import reactor.util.context.Context;
import reactor.util.context.ContextView;

public final class ErdOperationContexts {

  private static final Object METADATA_CONTEXT_KEY = new Object();
  private static final Object SUPPRESS_NESTED_MUTATION_CONTEXT_KEY = new Object();

  private ErdOperationContexts() {
    // utility class
  }

  public static ErdOperationMetadata metadata(ContextView contextView) {
    Object typedValue = contextView.getOrDefault(METADATA_CONTEXT_KEY, null);
    if (typedValue instanceof ErdOperationMetadata metadata) {
      return metadata;
    }
    return ErdOperationMetadata.empty();
  }

  public static Function<Context, Context> withSessionId(String sessionId) {
    return updateMetadata(metadata -> metadata.withSessionId(sessionId));
  }

  public static Function<Context, Context> withClientOperationId(String clientOperationId) {
    return updateMetadata(metadata -> metadata.withClientOperationId(clientOperationId));
  }

  public static Function<Context, Context> withBaseSchemaRevision(Long baseSchemaRevision) {
    return updateMetadata(metadata -> metadata.withBaseSchemaRevision(baseSchemaRevision));
  }

  public static Function<Context, Context> withActorUserId(String actorUserId) {
    return updateMetadata(metadata -> metadata.withActorUserId(actorUserId));
  }

  public static Function<Context, Context> suppressNestedMutation() {
    return context -> context.put(SUPPRESS_NESTED_MUTATION_CONTEXT_KEY, true);
  }

  public static boolean isNestedMutationSuppressed(ContextView contextView) {
    Object typedValue = contextView.getOrDefault(SUPPRESS_NESTED_MUTATION_CONTEXT_KEY, null);
    return typedValue instanceof Boolean suppressed && suppressed;
  }

  private static Function<Context, Context> updateMetadata(
      UnaryOperator<ErdOperationMetadata> updater) {
    return context -> context.put(METADATA_CONTEXT_KEY, updater.apply(metadata(context)));
  }

}
