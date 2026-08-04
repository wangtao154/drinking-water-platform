FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY target/ai-service-*.jar app.jar

EXPOSE 8101

ENTRYPOINT ["java", "-jar", "app.jar"]
