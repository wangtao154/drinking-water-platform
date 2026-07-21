FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY target/report-service-*.jar app.jar

ENV JAVA_OPTS="-Xmx512m -Xms256m -Duser.timezone=Asia/Shanghai"
ENV TZ=Asia/Shanghai

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
