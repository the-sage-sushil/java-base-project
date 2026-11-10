# syntax=docker/dockerfile:1
# Docker Hub times out on this network, so pull official images via the ECR mirror.
ARG REGISTRY=public.ecr.aws/docker/library

# ---- Stage 1: Angular build ------------------------------------------------
FROM ${REGISTRY}/node:22 AS frontend
WORKDIR /build/angular
COPY angular/package*.json ./
RUN npm ci
COPY angular/ ./
RUN npm run build
# angular.json outputPath -> /build/src/main/resources/static

# ---- Stage 2: Spring Boot build --------------------------------------------
FROM ${REGISTRY}/maven:3.9-eclipse-temurin-25 AS backend
WORKDIR /build
COPY pom.xml .
COPY .mvn .mvn
RUN mvn -B -DskipFrontend dependency:go-offline
COPY src src
COPY --from=frontend /build/src/main/resources/static src/main/resources/static
RUN mvn -B -DskipFrontend -DskipTests package

# ---- Stage 3: Runtime ------------------------------------------------------
FROM ${REGISTRY}/eclipse-temurin:25-jre AS runtime
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=backend /build/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]