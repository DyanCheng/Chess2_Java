# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace
COPY apps/server/pom.xml apps/server/pom.xml
COPY apps/server/src apps/server/src
COPY apps/web apps/web
RUN --mount=type=cache,target=/root/.m2 mvn -B -f apps/server/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -g 10001 spring && adduser -D -H -u 10001 -G spring spring
COPY --from=build --chown=spring:spring /workspace/apps/server/target/chess2-1.0.0.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -Dserver.port=${PORT:-8080} -jar app.jar"]
