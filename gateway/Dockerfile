FROM eclipse-temurin:17-jre-jammy
LABEL maintainer="drinking-water-platform"
LABEL service="gateway"

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY target/gateway-*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xmx384m -Xms128m -Duser.timezone=Asia/Shanghai"
ENV TZ=Asia/Shanghai

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
