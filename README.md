# SMS Checker / Frontend

The frontend allows users to interact with the model in the backend through a web-based UI.

The frontend is implemented with Spring Boot and only consists of a website and one REST endpoint.
It **requires Java 25+** to run (tested with 25.0.1).
Any classification requests will be delegated to the `backend` service that serves the model.
You must specify the environment variable `MODEL_HOST` to define where the backend is running.

The frontend service can be started through running the `Main` class (e.g., in your IDE) or through Maven (recommended):

    MODEL_HOST="http://localhost:8081" mvn spring-boot:run

The server runs on port 8080. Once its startup has finished, you can access [localhost:8080/sms](http://localhost:8080/sms) in your browser to interact with the application.

## Running the Frontend with Docker

First, build the Docker image. 

To be able to authenticate and download private packages from GitHub, you need to provide your credentials as build arguments.

**Make sure that the provided Personal Access Token (PAT) has (at least) read:packages permissions enabled!**

```bash
cd /path/to/project
docker build \
  --build-arg GITHUB_ACTOR=github_username \
  --build-arg GITHUB_TOKEN=github_token \
  -t sms-frontend:latest .
```

Then, run the container

```bash
docker run -p 8080:8080 \
  -e MODEL_HOST="http://localhost:8081" \
  sms-frontend:latest
```

Frontend will be available at [localhost:8080/sms](http://localhost:8080/sms).
