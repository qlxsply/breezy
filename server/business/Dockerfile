FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/app/ /app/

EXPOSE 8910

ENTRYPOINT ["java", "-Dlogging.config=/app/config/log4j2-spring.xml", "-Dapp.log4j.template.uri=file:/app/config/log4j2/json-template.json", "-Dapp.resources.dir=/app/app-resources/public", "-jar", "/app/app.jar"]
