# SMS Checker / Frontend

The frontend allows users to interact with the model in the backend through a web-based UI.

The frontend is implemented with Spring Boot and only consists of a website and one REST endpoint.
It **requires Java 25+** to run (tested with 25.0.1).
Any classification requests will be delegated to the `backend` service that serves the model.
You must specify the environment variable `MODEL_HOST` to define where the backend is running.

The frontend service can be started through running the `Main` class (e.g., in your IDE) or through Maven (recommended):

    MODEL_HOST="http://localhost:8081" mvn spring-boot:run

The server runs on port 8080. Once its startup has finished, you can access [localhost:8080/sms](http://localhost:8080/sms) in your browser to interact with the application.

### Required Setup (First Time Only)

**GitHub Packages requires authentication.** Configure it once, use it everywhere.

#### Step 1: Create GitHub Personal Access Token

1. Go to [GitHub Settings → Tokens](https://github.com/settings/tokens)
2. Click **"Generate new token (classic)"**
3. Configure and generate token
4. **Copy the token**

#### Step 2: Configure Maven Settings

Create or edit `~/.m2/settings.xml` on your computer with your `YOUR_GITHUB_USERNAME` and `YOUR_GITHUB_TOKEN`:
```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```


**Important:** The `<id>github</id>` matches the repository ID in this app's `pom.xml`, allowing Maven to automatically use these credentials when downloading lib-version.

## Running the Frontend with Docker
Build application:

```bash
mvn clean package -DskipTests
```

Build the Docker image:

```bash
docker build -t app:latest .
```

Run the container:

```bash
docker run -p 8080:8080 app:latest
```

Access the application at: http://localhost:8080/sms

Or you can specify the environment variables:

- `SERVER_PORT` - sets the port the frontend server runs on (default set to `8080`)
- `MODEL_HOST` - specifies where backend service is running (default set to `http://localhost:8081`)

For example

```bash
docker run -p 8085:8085 -e APP_PORT=8085 -e MODEL_HOST=http://localhost:8082 app:latest
```