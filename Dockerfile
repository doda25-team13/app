FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY target/*.jar app.jar

ENV SERVER_PORT=8080

ENV MODEL_HOST=http://model-service:8081

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${SERVER_PORT}"]
