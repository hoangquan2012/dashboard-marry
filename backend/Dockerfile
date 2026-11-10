# Multi-stage: Maven build with Flyway migrations from ../database/migration, then JRE runtime.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY database /src/database
COPY backend /src/backend
WORKDIR /src/backend
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 1001 wedding \
    && mkdir -p /app/uploads \
    && chown -R wedding:wedding /app
COPY --from=build /src/backend/target/wedding-management-backend.jar /app/app.jar
USER wedding
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=5s --retries=8 CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
