FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src src
RUN mvn clean package -DskipTests


# ---------------- RUNTIME ----------------
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/eve-helper-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-Xms256m", "-Xmx768m", "-XX:MaxMetaspaceSize=256m", "-Xlog:gc*:file=/app/gc.log:time,uptime:filecount=5,filesize=10M", "-jar", "app.jar"]
