package com.schemafy.core.project.application.access;

import java.lang.reflect.Method;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
class AccessAnnotationValidator implements SmartInitializingSingleton {

  private final ApplicationContext applicationContext;

  @Override
  public void afterSingletonsInstantiated() {
    applicationContext.getBeansOfType(Object.class, false, false)
        .values()
        .forEach(this::validateBean);
  }

  private void validateBean(Object bean) {
    Class<?> targetClass = AopUtils.getTargetClass(bean);
    for (Method method : targetClass.getDeclaredMethods()) {
      validateMethod(targetClass, method);
    }
  }

  private void validateMethod(Class<?> targetClass, Method method) {
    RequireProjectAccess projectAccess = method.getAnnotation(RequireProjectAccess.class);
    RequireWorkspaceAccess workspaceAccess = method.getAnnotation(RequireWorkspaceAccess.class);
    if (projectAccess == null && workspaceAccess == null) {
      return;
    }

    if (projectAccess != null && workspaceAccess != null) {
      throw new IllegalStateException(
          "Only one access annotation can be declared on "
              + targetClass.getSimpleName() + "#" + method.getName());
    }

    if (!Mono.class.isAssignableFrom(method.getReturnType())
        && !Flux.class.isAssignableFrom(method.getReturnType())) {
      throw new IllegalStateException(
          "Access annotations are only supported on Mono/Flux methods: "
              + targetClass.getSimpleName() + "#" + method.getName());
    }

    if (method.getParameterCount() != 1) {
      throw new IllegalStateException(
          "Access annotations require exactly one request parameter on "
              + targetClass.getSimpleName() + "#" + method.getName());
    }

    Class<?> requestType = method.getParameterTypes()[0];
    if (projectAccess != null) {
      validateStringAccessor(targetClass, method, requestType, projectAccess.projectId(), "projectId");
      validateStringAccessor(targetClass, method, requestType, projectAccess.requesterId(), "requesterId");
    }
    if (workspaceAccess != null) {
      validateStringAccessor(targetClass, method, requestType, workspaceAccess.workspaceId(), "workspaceId");
      validateStringAccessor(targetClass, method, requestType, workspaceAccess.requesterId(), "requesterId");
    }
  }

  private void validateStringAccessor(
      Class<?> targetClass,
      Method method,
      Class<?> requestType,
      String accessorName,
      String label) {
    Method accessor;
    try {
      accessor = requestType.getMethod(accessorName);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(
          "Access annotation requires " + label + " accessor " + accessorName + "() on "
              + requestType.getSimpleName() + " for "
              + targetClass.getSimpleName() + "#" + method.getName(),
          e);
    }
    if (!String.class.equals(accessor.getReturnType())) {
      throw new IllegalStateException(
          "Access annotation requires " + label + " accessor " + accessorName
              + " to return String on "
              + targetClass.getSimpleName() + "#" + method.getName());
    }
  }

}
