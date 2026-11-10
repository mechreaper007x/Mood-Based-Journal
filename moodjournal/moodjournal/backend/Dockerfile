# ─── Stage 1: Build and test the JAR ──────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline -B --no-transfer-progress -q

COPY src/ src/
# Do not publish an image when the backend test suite is failing.
RUN mvn verify -B --no-transfer-progress

# ─── Stage 2: Minimal JRE runtime ──────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN mkdir -p /app/data \
    && adduser -D -u 1000 appuser \
    && chown -R appuser:appuser /app
USER appuser

COPY --from=builder --chown=appuser:appuser /app/target/moodjournal-*.jar /app/app.jar

# H2 is retained only as a local/single-instance fallback. Production should
# provide a managed PostgreSQL connection and a persistent backup strategy.
ENV SPRING_DATASOURCE_URL=jdbc:h2:file:/app/data/moodjournal;AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE
ENV SPRING_DATASOURCE_USERNAME=sa
ENV SPRING_DATASOURCE_PASSWORD=
ENV SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect
ENV H2_CONSOLE_ENABLED=false
ENV PORT=7860
EXPOSE 7860

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
