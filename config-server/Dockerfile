# Stage 1: Build
FROM maven:3-eclipse-temurin-21-alpine AS build
WORKDIR /app
# hanya copy folder config-server dari monorepo
COPY config-server /app
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
