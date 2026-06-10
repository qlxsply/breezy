FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG JAR_FILE=target/*-exec.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8910

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
