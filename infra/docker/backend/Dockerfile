# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY gradlew settings.gradle ./
COPY gradle/ gradle/
COPY apps/backend/core/ apps/backend/core/
COPY apps/backend/domain/ apps/backend/domain/
COPY apps/backend/sql-resources/ apps/backend/sql-resources/

RUN chmod +x gradlew && ./gradlew :core:bootJar --no-daemon -x test -x spotbugsMain -x spotlessCheck

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/apps/backend/core/build/libs/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

ENV SPRING_PROFILES_ACTIVE=server
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=40s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
