FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

# Install Docker CLI and Compose plugin from default repos
RUN apt-get update && apt-get install -y docker.io docker-compose && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/target/*.jar app.jar
COPY scripts/ scripts/
RUN chmod +x scripts/*.sh
ENTRYPOINT ["java", "-jar", "app.jar"]
