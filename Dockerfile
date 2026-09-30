# Backend image:  docker build -t bfsi-backend .
# (the whole stack, database and web UI included:  docker compose up --build)

# ---- build: compile the Spring Boot jar ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY java ./java
COPY resources ./resources
RUN mvn -B -q -DskipTests package

# ---- run: small JRE image, non-root ----
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER app

# production profile: every secret (DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET) must be passed in as env vars
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080

# probes live on the internal management port (application-prod.properties), never on the public one
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -qO- http://localhost:${MANAGEMENT_PORT:-8081}/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
