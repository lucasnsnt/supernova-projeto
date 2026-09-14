FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw --batch-mode dependency:go-offline
COPY src src
RUN ./mvnw --batch-mode -DskipTests package

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 supernova \
    && useradd --system --uid 10001 --gid supernova --home-dir /app supernova

WORKDIR /app
COPY --from=builder --chown=supernova:supernova /workspace/target/*.jar app.jar

USER 10001:10001
EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl --fail --silent http://127.0.0.1:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
