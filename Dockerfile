# =============================================
# Dockerfile — Build multi-stage
# Estágio 1: compila com Maven
# Estágio 2: imagem final enxuta com o JAR
# =============================================

# ── Estágio 1: Compilação ────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

# Fix: instala Maven primeiro, depois compila
RUN apk add --no-cache maven

WORKDIR /build

# Copia pom.xml e baixa dependências (cache de layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Compila o projeto
COPY src ./src
RUN mvn package -DskipTests -B -q

# ── Estágio 2: Runtime ───────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S sesmt && adduser -S pgeo -G sesmt

WORKDIR /app
RUN mkdir -p /app/logs && chown pgeo:sesmt /app/logs

COPY --from=builder /build/target/pgeo-*.jar app.jar

USER pgeo

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health 2>/dev/null | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", \
    "-Xms256m", \
    "-Xmx512m", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=70.0", \
    "-Dspring.profiles.active=prod", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
