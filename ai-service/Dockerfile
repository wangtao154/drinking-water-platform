FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY target/ai-service-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8101

ENTRYPOINT ["java", "-jar", "app.jar"]
