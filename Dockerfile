FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies separately from source so `docker compose up` rebuilds
# stay fast when only application code changes.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Same relative path (android/release/app.apk) application.yml's default
# giglister.app.download-path already expects, so no env var override is
# needed here either - see AppDownloadController.
COPY android/release/app.apk ./android/release/app.apk

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
