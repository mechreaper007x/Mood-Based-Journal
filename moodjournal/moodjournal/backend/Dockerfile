# ─── Stage 1: Build the JAR ───────────────────────────────────────────────────
# Standard Maven + JDK 21 image — proven reliable on HF Spaces
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Pre-cache dependencies (separate layer = faster re-builds when only src changes)
COPY pom.xml ./
RUN mvn dependency:go-offline -B --no-transfer-progress -q

# Build the JAR (skip tests)
COPY src/ src/
RUN mvn package -DskipTests -B --no-transfer-progress -q

# ─── Stage 2: Minimal JRE runtime ────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Data directory for H2 file-based DB
RUN mkdir -p /app/data

# HF Spaces requires uid 1000 non-root user
RUN adduser -D -u 1000 appuser && chown -R appuser:appuser /app
USER appuser

# Copy built JAR
COPY --from=builder --chown=appuser:appuser /app/target/moodjournal-*.jar /app/app.jar

# H2 embedded DB defaults (no PostgreSQL on HF)
ENV SPRING_DATASOURCE_URL=jdbc:h2:file:/app/data/moodjournal;AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE
ENV SPRING_DATASOURCE_USERNAME=sa
ENV SPRING_DATASOURCE_PASSWORD=
ENV SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect
ENV H2_CONSOLE_ENABLED=false

# HF Spaces port
ENV PORT=7860
EXPOSE 7860

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
