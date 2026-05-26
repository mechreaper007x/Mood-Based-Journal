# ─── Stage 1: Build the JAR with Maven ───────────────────────────────────────
# Uses GraalVM JDK 21 (great JVM performance even without native compilation)
# Native image compilation requires 4-8GB RAM which HF free tier doesn't have.
# The pom.xml native profile is still available for local native builds.
FROM ghcr.io/graalvm/jdk-community:21 AS builder
WORKDIR /app

# Pre-cache Maven dependencies (separate layer = faster rebuilds)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B --no-transfer-progress

# Copy source and build the JAR (skip tests, skip native compilation)
COPY src/ src/
RUN ./mvnw package -DskipTests -B --no-transfer-progress

# ─── Stage 2: Minimal JRE runtime ────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create data dir for H2 file-based database
RUN mkdir -p /app/data

# Hugging Face Spaces REQUIRES uid 1000 non-root user
RUN adduser -D -u 1000 appuser && chown -R appuser:appuser /app
USER appuser

# Copy the built JAR
COPY --from=builder --chown=appuser:appuser /app/target/moodjournal-*.jar /app/moodjournal.jar

# Default to H2 embedded DB (no external PostgreSQL on HF)
ENV SPRING_DATASOURCE_URL=jdbc:h2:file:/app/data/moodjournal;AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE
ENV SPRING_DATASOURCE_USERNAME=sa
ENV SPRING_DATASOURCE_PASSWORD=
ENV SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect
ENV H2_CONSOLE_ENABLED=false

# Hugging Face Spaces exposes port 7860
ENV PORT=7860
EXPOSE 7860

# Run with GC tuning for container environments
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/moodjournal.jar"]
