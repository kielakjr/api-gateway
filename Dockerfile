# ── Build stage ────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Cache dependencies by copying pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build the fat JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Runtime stage ──────────────────────────────────────────────
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the fat JAR from the build stage
COPY --from=build /build/target/api-gateway-1.0-SNAPSHOT.jar app.jar

# Copy default config
COPY config.yaml .

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
