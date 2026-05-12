# Build stage — compiles the fat JAR without running tests
# (tests run in the CI pipeline's dedicated gradle-build step)
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :game-api:bootJar --no-daemon -x test

# Runtime stage — minimal JRE image
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/game-api/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
