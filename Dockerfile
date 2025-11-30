FROM maven:3.8.5-openjdk-17-slim AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src src
RUN mvn clean package -DskipTests


# ---- Runtime image ----
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=builder /app/target/eve-helper-backend-0.0.1-SNAPSHOT.jar eve-helper-backend.jar

EXPOSE 8080
CMD ["java", "-jar", "eve-helper-backend.jar"]
