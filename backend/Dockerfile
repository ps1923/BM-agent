FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY backend/pom.xml pom.xml
RUN mvn -q -DskipTests dependency:go-offline
COPY backend/src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 bmhs
COPY --from=build /workspace/target/bm-hs-api-0.1.0.jar app.jar
USER bmhs
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
