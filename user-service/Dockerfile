FROM eclipse-temurin:17-jre-jammy
LABEL maintainer="drinking-water-platform"
LABEL service="user-service"

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY target/user-service-*.jar app.jar

EXPOSE 8082

ENV JAVA_OPTS="-Xmx512m -Xms256m -Duser.timezone=Asia/Shanghai"
ENV TZ=Asia/Shanghai

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
