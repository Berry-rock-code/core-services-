# Build Stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app
COPY pom.xml .
# Download dependencies (cache layer)
RUN mvn dependency:go-offline -B

COPY src ./src
# Build the application
RUN mvn clean package -DskipTests

# Run Stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /app/target/integration-hub-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
