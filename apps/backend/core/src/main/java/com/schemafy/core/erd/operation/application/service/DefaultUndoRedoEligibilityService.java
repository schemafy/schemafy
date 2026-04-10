package com.schemafy.core.erd.operation.application.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.operation.application.port.out.GetErdOperationByIdPort;
import com.schemafy.core.erd.operation.application.port.out.GetErdOperationsBySchemaIdPort;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.exception.OperationErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class DefaultUndoRedoEligibilityService implements UndoRedoEligibilityService {

  private final GetErdOperationByIdPort getErdOperationByIdPort;
  private final GetErdOperationsBySchemaIdPort getErdOperationsBySchemaIdPort;

  @Override
  public Mono<ResolvedUndoRedoEligibility> resolve(UndoRedoAction action, String targetOpId) {
    Objects.requireNonNull(action, "action");

    if (!StringUtils.hasText(targetOpId)) {
      return Mono.error(new DomainException(
          OperationErrorCode.INVALID_VALUE,
          "targetOpId must not be blank"));
    }

    return getErdOperationByIdPort.findOperationById(targetOpId)
        .switchIfEmpty(Mono.error(new DomainException(
            OperationErrorCode.NOT_FOUND,
            "Operation not found: " + targetOpId)))
        .flatMap(targetOperation -> getErdOperationsBySchemaIdPort
            .findOperationsBySchemaIdOrderByCommittedRevisionAsc(targetOperation.schemaId())
            .map(operations -> resolveFromHistory(action, targetOperation, operations)));
  }

  private ResolvedUndoRedoEligibility resolveFromHistory(
      UndoRedoAction action,
      ErdOperationLog targetOperation,
      List<ErdOperationLog> operations) {
    LinearHistoryState history = replayHistory(operations);
    ErdOperationLog targetRootOriginalOperation = history.rootByOperationId().get(targetOperation.opId());

    if (targetRootOriginalOperation == null) {
      throw new IllegalStateException(
          "Target operation missing from linear history: opId=" + targetOperation.opId());
    }

    ErdOperationLog currentChainTipOperation = history.currentChainTipByRootId()
        .get(targetRootOriginalOperation.opId());

    if (currentChainTipOperation == null) {
      throw new IllegalStateException(
          "Current chain tip missing for root operation: opId=" + targetRootOriginalOperation.opId());
    }

    ResolvedUndoRedoEligibility resolved = new ResolvedUndoRedoEligibility(
        action,
        targetOperation,
        targetRootOriginalOperation,
        currentChainTipOperation,
        history.currentUndoCandidate(),
        history.currentRedoCandidate(),
        history.schemaHeadOperation());

    validateEligibility(resolved, history);
    return resolved;
  }

  private void validateEligibility(
      ResolvedUndoRedoEligibility resolved,
      LinearHistoryState history) {
    String targetRootOpId = resolved.targetRootOriginalOperation().opId();

    if (resolved.action() == UndoRedoAction.UNDO) {
      if (sameOperation(resolved.currentUndoCandidateOperation(), resolved.targetRootOriginalOperation())) {
        return;
      }
      if (history.redoStackRootIds().contains(targetRootOpId)) {
        throw new DomainException(
            OperationErrorCode.ALREADY_UNDONE,
            "Operation is already undone: opId=" + targetRootOpId);
      }
      throw new DomainException(
          OperationErrorCode.SUPERSEDED,
          "Operation is superseded by a newer schema revision: opId=" + targetRootOpId);
    }

    if (sameOperation(resolved.currentRedoCandidateOperation(), resolved.targetRootOriginalOperation())) {
      return;
    }

    throw new DomainException(
        OperationErrorCode.REDO_NOT_ELIGIBLE,
        "Operation is not eligible for redo: opId=" + targetRootOpId);
  }

  private LinearHistoryState replayHistory(List<ErdOperationLog> operations) {
    if (operations.isEmpty()) {
      throw new IllegalStateException("Linear undo/redo history requires at least one operation");
    }

    Deque<ErdOperationLog> undoStack = new ArrayDeque<>();
    Deque<ErdOperationLog> redoStack = new ArrayDeque<>();
    Map<String, ErdOperationLog> rootByOperationId = new LinkedHashMap<>();
    Map<String, ErdOperationLog> currentChainTipByRootId = new LinkedHashMap<>();
    ErdOperationLog schemaHeadOperation = null;

    for (ErdOperationLog operation : operations) {
      schemaHeadOperation = operation;

      if (operation.derivationKind() == ErdOperationDerivationKind.ORIGINAL) {
        rootByOperationId.put(operation.opId(), operation);
        currentChainTipByRootId.put(operation.opId(), operation);
        undoStack.push(operation);
        redoStack.clear();
        continue;
      }

      ErdOperationLog rootOriginalOperation = resolveRootOperation(operation, rootByOperationId);
      rootByOperationId.put(operation.opId(), rootOriginalOperation);
      currentChainTipByRootId.put(rootOriginalOperation.opId(), operation);

      if (operation.derivationKind() == ErdOperationDerivationKind.UNDO) {
        applyUndoOperation(operation, rootOriginalOperation, undoStack, redoStack);
        continue;
      }

      if (operation.derivationKind() == ErdOperationDerivationKind.REDO) {
        applyRedoOperation(operation, rootOriginalOperation, undoStack, redoStack);
        continue;
      }

      throw new IllegalStateException("Unsupported derivation kind: " + operation.derivationKind());
    }

    Set<String> redoStackRootIds = redoStack.stream()
        .map(ErdOperationLog::opId)
        .collect(Collectors.toUnmodifiableSet());

    return new LinearHistoryState(
        rootByOperationId,
        currentChainTipByRootId,
        List.copyOf(undoStack),
        List.copyOf(redoStack),
        redoStackRootIds,
        schemaHeadOperation);
  }

  private ErdOperationLog resolveRootOperation(
      ErdOperationLog operation,
      Map<String, ErdOperationLog> rootByOperationId) {
    String derivedFromOpId = operation.derivedFromOpId();

    if (derivedFromOpId == null) {
      throw new IllegalStateException(
          "Derived operation must reference a parent operation: opId=" + operation.opId());
    }

    ErdOperationLog rootOriginalOperation = rootByOperationId.get(derivedFromOpId);

    if (rootOriginalOperation == null) {
      throw new IllegalStateException(
          "Derived operation parent is missing from history: opId=" + operation.opId()
              + ", derivedFromOpId=" + derivedFromOpId);
    }

    return rootOriginalOperation;
  }

  private void applyUndoOperation(
      ErdOperationLog operation,
      ErdOperationLog rootOriginalOperation,
      Deque<ErdOperationLog> undoStack,
      Deque<ErdOperationLog> redoStack) {
    ErdOperationLog currentUndoCandidate = undoStack.peek();

    if (!sameOperation(currentUndoCandidate, rootOriginalOperation)) {
      throw new IllegalStateException(
          "Undo operation must target the current undo candidate: opId=" + operation.opId()
              + ", expectedRootOpId="
              + (currentUndoCandidate == null ? "<none>" : currentUndoCandidate.opId())
              + ", actualRootOpId=" + rootOriginalOperation.opId());
    }

    undoStack.pop();
    redoStack.push(rootOriginalOperation);
  }

  private void applyRedoOperation(
      ErdOperationLog operation,
      ErdOperationLog rootOriginalOperation,
      Deque<ErdOperationLog> undoStack,
      Deque<ErdOperationLog> redoStack) {
    ErdOperationLog currentRedoCandidate = redoStack.peek();

    if (!sameOperation(currentRedoCandidate, rootOriginalOperation)) {
      throw new IllegalStateException(
          "Redo operation must target the current redo candidate: opId=" + operation.opId()
              + ", expectedRootOpId="
              + (currentRedoCandidate == null ? "<none>" : currentRedoCandidate.opId())
              + ", actualRootOpId=" + rootOriginalOperation.opId());
    }

    redoStack.pop();
    undoStack.push(rootOriginalOperation);
  }

  private boolean sameOperation(ErdOperationLog left, ErdOperationLog right) {
    return left != null && right != null && left.opId().equals(right.opId());
  }

  private record LinearHistoryState(
      Map<String, ErdOperationLog> rootByOperationId,
      Map<String, ErdOperationLog> currentChainTipByRootId,
      List<ErdOperationLog> undoStack,
      List<ErdOperationLog> redoStack,
      Set<String> redoStackRootIds,
      ErdOperationLog schemaHeadOperation) {

    ErdOperationLog currentUndoCandidate() {
      return undoStack.isEmpty() ? null : undoStack.getFirst();
    }

    ErdOperationLog currentRedoCandidate() {
      return redoStack.isEmpty() ? null : redoStack.getFirst();
    }

  }

}
