FROM maven:3.9-eclipse-temurin-25-alpine AS base

######################
# STAGE 1: Get Dependencies
FROM base AS dependencies

WORKDIR /app

ARG GITHUB_ACTOR
ARG GITHUB_TOKEN

RUN mkdir -p /root/.m2 && \
    cat <<EOF > /root/.m2/settings.xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>${GITHUB_ACTOR}</username>
      <password>${GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
EOF

COPY pom.xml .

# Download parent pom and transitive dependencies + other needed Maven plugins for mvn package to work offline
RUN mvn -s /root/.m2/settings.xml -B -Dmaven.repo.local=.m2repo dependency:resolve dependency:resolve-plugins

######################
# STAGE 2: Build
FROM base AS build

WORKDIR /app

COPY --from=dependencies /app /app
COPY --from=dependencies /root/.m2 /root/.m2

COPY src ./src

# skip tests for faster build and give location of local Maven repository
RUN mvn -s /root/.m2/settings.xml -o -DskipTests -Dmaven.repo.local=.m2repo package

######################
# STAGE 3: Run

# Use smaller image
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV SERVER_PORT=8080

ENV MODEL_HOST=http://model-service:8081

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${SERVER_PORT}"]
