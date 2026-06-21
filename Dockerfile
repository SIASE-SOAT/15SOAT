# ---- Build Stage ----
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copia POMs e baixa dependencias (cache layer)
COPY pom.xml ./
COPY siase-domain/pom.xml ./siase-domain/
COPY siase-application/pom.xml ./siase-application/
COPY siase-infrastructure/pom.xml ./siase-infrastructure/

# Download dependencies (aproveita cache do Docker)
RUN mvn dependency:go-offline -B -pl siase-infrastructure -am

# Copia codigo-fonte e faz build multi-modulo
COPY siase-domain/src ./siase-domain/src
COPY siase-application/src ./siase-application/src
COPY siase-infrastructure/src ./siase-infrastructure/src
RUN mvn clean package -DskipTests -B -pl siase-infrastructure -am

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

COPY --from=builder /app/siase-infrastructure/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-jar", "app.jar"]
