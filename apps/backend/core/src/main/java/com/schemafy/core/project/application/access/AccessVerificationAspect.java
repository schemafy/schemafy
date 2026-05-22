package com.schemafy.core.project.application.access;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import com.schemafy.core.erd.operation.ErdOperationContexts;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AccessVerificationAspect {

  private final AccessVerifier accessVerifier;
  private final ProjectAccessTargetInference targetInference;
  private final ErdProjectContextResolver erdProjectContextResolver;

  @Around("@annotation(com.schemafy.core.project.application.access.RequireProjectAccess) || "
      + "@within(com.schemafy.core.project.application.access.RequireProjectAccess) || "
      + "@annotation(com.schemafy.core.project.application.access.RequireWorkspaceAccess)")
  public Object verifyAccess(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = AopUtils.getMostSpecificMethod(
        signature.getMethod(),
        joinPoint.getTarget().getClass());

    RequireProjectAccess projectAccess = resolveProjectAccess(method, joinPoint.getTarget().getClass());
    RequireWorkspaceAccess workspaceAccess = method.getAnnotation(RequireWorkspaceAccess.class);
    if (projectAccess != null && workspaceAccess != null) {
      throw new IllegalStateException(
          "Only one access annotation can be declared on "
              + method.getDeclaringClass().getSimpleName() + "#" + method.getName());
    }
    if (projectAccess == null && workspaceAccess == null) {
      throw new IllegalStateException(
          "Access annotation is missing on "
              + method.getDeclaringClass().getSimpleName() + "#" + method.getName());
    }

    Class<?> returnType = method.getReturnType();
    if (Mono.class.isAssignableFrom(returnType)) {
      return Mono.defer(() -> resolveAccessRequest(
          method,
          joinPoint.getArgs(),
          projectAccess,
          workspaceAccess)
          .flatMap(accessRequest -> Mono.defer(() -> proceedMono(joinPoint, method))
              .contextWrite(ProjectAccessRequesterContext.withRequesterId(
                  accessRequest.requesterId()))));
    }
    if (Flux.class.isAssignableFrom(returnType)) {
      return Flux.defer(() -> resolveAccessRequest(
          method,
          joinPoint.getArgs(),
          projectAccess,
          workspaceAccess)
          .flatMapMany(accessRequest -> Flux.defer(() -> proceedFlux(joinPoint, method))
              .contextWrite(ProjectAccessRequesterContext.withRequesterId(
                  accessRequest.requesterId()))));
    }
    throw new IllegalStateException(
        "Access annotations are only supported on Mono/Flux methods: "
            + method.getDeclaringClass().getSimpleName() + "#" + method.getName());
  }

  private RequireProjectAccess resolveProjectAccess(Method method, Class<?> targetClass) {
    RequireProjectAccess methodAccess = method.getAnnotation(RequireProjectAccess.class);
    if (methodAccess != null) {
      return methodAccess;
    }
    return targetClass.getAnnotation(RequireProjectAccess.class);
  }

  private Mono<AccessRequest> resolveAccessRequest(
      Method method,
      Object[] args,
      RequireProjectAccess projectAccess,
      RequireWorkspaceAccess workspaceAccess) {
    Object request = extractRequestArgument(method, args);
    if (projectAccess != null) {
      List<ProjectAccessTarget> targets = targetInference
          .resolveTargets(projectAccess, request.getClass());
      if (!targets.isEmpty()) {
        return verifyProjectResourceTargets(method, request, projectAccess, targets);
      }
      if (targetInference.isErdType(request.getClass())) {
        return Mono.error(new IllegalStateException(
            "ERD project access target is missing on "
                + method.getDeclaringClass().getSimpleName() + "#" + method.getName()));
      }
      AccessRequest accessRequest = extractAccessRequest(
          method,
          request,
          projectAccess.projectId(),
          projectAccess.requesterId(),
          "projectId");
      return accessVerifier.requireProjectAccess(
          accessRequest.resourceId(),
          accessRequest.requesterId(),
          projectAccess.role())
          .thenReturn(accessRequest);
    }
    AccessRequest accessRequest = extractAccessRequest(
        method,
        request,
        workspaceAccess.workspaceId(),
        workspaceAccess.requesterId(),
        "workspaceId");
    return accessVerifier.requireWorkspaceAccess(
        accessRequest.resourceId(),
        accessRequest.requesterId(),
        workspaceAccess.role())
        .thenReturn(accessRequest);
  }

  private AccessRequest extractAccessRequest(
      Method method,
      Object request,
      String resourceAccessorName,
      String requesterAccessorName,
      String resourceLabel) {
    return new AccessRequest(
        extractStringValue(method, request, resourceAccessorName, resourceLabel),
        extractStringValue(method, request, requesterAccessorName, "requesterId"));
  }

  private Mono<AccessRequest> verifyProjectResourceTargets(
      Method method,
      Object request,
      RequireProjectAccess projectAccess,
      List<ProjectAccessTarget> targets) {
    if (erdProjectContextResolver == null) {
      return Mono.error(new IllegalStateException(
          "ERD project context resolver is required for "
              + method.getDeclaringClass().getSimpleName() + "#" + method.getName()));
    }
    return Mono.deferContextual(contextView -> {
      String contextRequesterId = ProjectAccessRequesterContext.requesterIdOrNull(contextView);
      String requesterId = contextRequesterId != null
          ? contextRequesterId
          : resolveRequesterId(method, request, projectAccess.requesterId(), contextView);
      if (requesterId == null) {
        return Mono.error(new IllegalStateException("Project access requester is missing"));
      }
      Map<ProjectAccessResourceRef, Mono<String>> projectIdCache = new HashMap<>();
      return Flux.fromIterable(targets)
          .concatMap(target -> erdProjectContextResolver.resolveProjectId(
              target.type(),
              extractStringValue(method, request, target.accessorName(), "target"),
              projectIdCache))
          .distinct()
          .concatMap(projectId -> accessVerifier.requireProjectAccess(
              projectId, requesterId, projectAccess.role()))
          .then(Mono.just(new AccessRequest(null, requesterId)));
    });
  }

  private String resolveRequesterId(
      Method method,
      Object request,
      String requesterAccessorName,
      ContextView contextView) {
    String actorUserId = ErdOperationContexts.metadata(contextView).actorUserIdOr(null);
    if (actorUserId != null) {
      return actorUserId;
    }
    try {
      Method requesterId = request.getClass().getMethod(requesterAccessorName);
      Object value = requesterId.invoke(request);
      return value instanceof String stringValue ? stringValue : null;
    } catch (NoSuchMethodException e) {
      return null;
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(
          "Access annotation cannot read requesterId accessor " + requesterAccessorName + " on "
              + method.getDeclaringClass().getSimpleName() + "#" + method.getName(),
          e);
    } catch (InvocationTargetException e) {
      throw new IllegalStateException(
          "Access annotation requesterId accessor " + requesterAccessorName + " failed on "
              + method.getDeclaringClass().getSimpleName() + "#" + method.getName(),
          e.getTargetException());
    }
  }

  @SuppressWarnings("unchecked")
  private Mono<Object> proceedMono(ProceedingJoinPoint joinPoint, Method method) {
    try {
      Object result = joinPoint.proceed();
      if (result instanceof Mono<?> mono) {
        return (Mono<Object>) mono;
      }
      return Mono.error(new IllegalStateException(
          "Access annotation target did not return Mono: "
              + method.getDeclaringClass().getSimpleName() + "#" + method.getName()));
    } catch (Throwable t) {
      return Mono.error(t);
    }
  }

  @SuppressWarnings("unchecked")
  private Flux<Object> proceedFlux(ProceedingJoinPoint joinPoint, Method method) {
    try {
      Object result = joinPoint.proceed();
      if (result instanceof Flux<?> flux) {
        return (Flux<Object>) flux;
      }
      return Flux.error(new IllegalStateException(
          "Access annotation target did not return Flux: "
              + method.getDeclaringClass().getSimpleName() + "#" + method.getName()));
    } catch (Throwable t) {
      return Flux.error(t);
    }
  }

  private String extractStringValue(Method method, Object request, String accessorName, String label) {
    Method accessor = findAccessor(method, request.getClass(), accessorName, label);
    Object value;
    try {
      value = accessor.invoke(request);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(
          "Access annotation cannot read " + label + " accessor " + accessorName + " on "
              + method.getDeclaringClass().getSimpleName() + "#" + method.getName(),
          e);
    } catch (InvocationTargetException e) {
      throw new IllegalStateException(
          "Access annotation accessor " + accessorName + " failed on "
              + method.getDeclaringClass().getSimpleName() + "#" + method.getName(),
          e.getTargetException());
    }
    if (!(value instanceof String stringValue)) {
      throw new IllegalStateException(
          "Access annotation requires " + label + " accessor " + accessorName
              + " to return String on "
              + method.getDeclaringClass().getSimpleName() + "#" + method.getName());
    }
    return stringValue;
  }

  private Object extractRequestArgument(Method method, Object[] args) {
    if (args.length != 1 || args[0] == null) {
      throw new IllegalStateException(
          "Access annotations require exactly one non-null request argument on "
              + method.getDeclaringClass().getSimpleName() + "#" + method.getName());
    }
    return args[0];
  }

  private Method findAccessor(Method method, Class<?> requestType, String accessorName, String label) {
    try {
      return requestType.getMethod(accessorName);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(
          "Access annotation requires " + label + " accessor " + accessorName + "() on "
              + requestType.getSimpleName() + " for "
              + method.getDeclaringClass().getSimpleName() + "#" + method.getName(),
          e);
    }
  }

  private record AccessRequest(String resourceId, String requesterId) {
  }

}
