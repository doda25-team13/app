######################
# STAGE 1: Get Dependencies
FROM maven:3.9-eclipse-temurin-25 AS dependencies

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B


######################
# STAGE 2: Build
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

COPY --from=dependencies /app /app

COPY src ./src

# skip tests for faster build
RUN mvn package -DskipTests

######################
# STAGE 3: Run

# Use smaller image
FROM eclipse-temurin:25-jdk

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV SERVER_PORT=8080

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]