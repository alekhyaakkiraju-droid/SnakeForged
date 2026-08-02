# Build stage — compiles the fat JAR without running tests
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x ./gradlew && ./gradlew :game-api:bootJar --no-daemon -x test

# Runtime stage — minimal JRE image
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN mkdir -p /app/data && chown -R 1000:1000 /app
USER 1000:1000
COPY --from=build --chown=1000:1000 /workspace/game-api/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
