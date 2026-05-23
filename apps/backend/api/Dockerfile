# syntax=docker/dockerfile:1.7
# Build context must be the repository root (multi-module Gradle build).

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /src

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY apps/backend ./apps/backend

RUN chmod +x gradlew \
 && ./gradlew --no-daemon :api:bootJar -x test -x spotbugsMain -x spotlessCheck

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home /app app

COPY --from=build /src/apps/backend/api/build/libs/api-*.jar /app/app.jar
RUN find /app -name 'api-*-plain.jar' -delete || true

USER app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
ENV SPRING_PROFILES_ACTIVE=server
ENV SERVER_PORT=8080

EXPOSE 8080
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
