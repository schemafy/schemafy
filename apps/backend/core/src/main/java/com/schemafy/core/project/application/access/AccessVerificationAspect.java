package com.schemafy.core.project.application.access;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AccessVerificationAspect {

  private final AccessVerifier accessVerifier;

  @Around("@annotation(com.schemafy.core.project.application.access.RequireProjectAccess) || "
      + "@annotation(com.schemafy.core.project.application.access.RequireWorkspaceAccess)")
  public Object verifyAccess(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = AopUtils.getMostSpecificMethod(
        signature.getMethod(),
        joinPoint.getTarget().getClass());

    RequireProjectAccess projectAccess = method.getAnnotation(RequireProjectAccess.class);
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
      return Mono.defer(() -> buildVerificationChain(
          method,
          joinPoint.getArgs(),
          projectAccess,
          workspaceAccess)
          .then(Mono.defer(() -> proceedMono(joinPoint, method))));
    }
    if (Flux.class.isAssignableFrom(returnType)) {
      return Flux.defer(() -> buildVerificationChain(
          method,
          joinPoint.getArgs(),
          projectAccess,
          workspaceAccess)
          .thenMany(Flux.defer(() -> proceedFlux(joinPoint, method))));
    }
    throw new IllegalStateException(
        "Access annotations are only supported on Mono/Flux methods: "
            + method.getDeclaringClass().getSimpleName() + "#" + method.getName());
  }

  private Mono<Void> buildVerificationChain(
      Method method,
      Object[] args,
      RequireProjectAccess projectAccess,
      RequireWorkspaceAccess workspaceAccess) {
    return projectAccess != null
        ? accessVerifier.requireProjectAccess(
            extractStringValue(method, args, projectAccess.projectId(), "projectId"),
            extractStringValue(method, args, projectAccess.requesterId(), "requesterId"),
            projectAccess.role())
        : accessVerifier.requireWorkspaceAccess(
            extractStringValue(method, args, workspaceAccess.workspaceId(), "workspaceId"),
            extractStringValue(method, args, workspaceAccess.requesterId(), "requesterId"),
            workspaceAccess.role());
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

  private String extractStringValue(Method method, Object[] args, String accessorName, String label) {
    Object request = extractRequestArgument(method, args);
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

}
