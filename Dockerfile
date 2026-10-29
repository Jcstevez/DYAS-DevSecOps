# Imagen de la API de reservas. Multi-etapa: una etapa compila con Maven y otra solo ejecuta con el JRE.
# Las imágenes base tienen versión fija; Dependabot (.github/dependabot.yml) propone las actualizaciones.

# ---------- Etapa 1: compilar ----------
FROM maven:3.9.16-eclipse-temurin-21-noble AS build
WORKDIR /app

# Primero solo el pom.xml: si las dependencias no cambian, Docker reutiliza esta capa.
COPY pom.xml .
RUN mvn -B -q -ntp dependency:go-offline

# Luego el código. Las pruebas no se repiten aquí: ya corrieron en el pipeline (mvn verify).
COPY src ./src
RUN mvn -B -q -ntp package -DskipTests

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:24.0.2_12-jre-noble

# Usuario sin privilegios con un UID fijo: el proceso no corre como root dentro del contenedor.
RUN groupadd --system --gid 10001 reservas \
 && useradd --system --uid 10001 --gid reservas --no-create-home --shell /usr/sbin/nologin reservas

# Versión que se construyó (la define el pipeline). Se publica en /actuator/info y la verifica la prueba de humo.
ARG VERSION=local
ENV APP_VERSION=${VERSION}

WORKDIR /app
COPY --from=build /app/target/reservas.jar app.jar
USER 10001:10001

EXPOSE 8080

# Docker y Compose consultan este endpoint para saber si el servicio está listo.
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
