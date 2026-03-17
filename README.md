# Integration Hub (Java Edition)

The **Integration Hub** is a core, unified platform service designed to orchestrate and unify automations across Berry Rock's technical infrastructure (e.g., Buildium, Google Sheets, Salesforce). 

This service has been recently refactored into a proper Java 21 + Spring Boot + Maven project. It serves as an enterprise-grade foundation built for maintainability, clarity, and future compliance audits (e.g., SOC2), running seamlessly with standard backend conventions.

## Why Java & Spring Boot?
Java and Spring Boot were chosen for this foundational layer to provide:
- **Predictability & Structure:** Strict typing and standard application layering.
- **Enterprise Capabilities:** Out-of-the-box configuration handling, Actuator metrics, and standard DI.
- **Maintainability:** Clear separation between Http handling (`Controller`), application logic (`Service`), and external systems (`Client`). This ensures that the code scales smoothly as integrations grow.

## Package Architecture
- `config`: Spring Boot configurations and bean definitions.
- `controller`: HTTP endpoints. Logic here should be strictly limited to routing, basic validation, and responding.
- `service`: Core business orchestration and rule handling.
- `client`: Thin interfaces and wrappers around vendor APIs (Buildium, Google Sheets, etc.).
- `model` / `dto`: Domain objects and API response representations.
- `audit`: Centralized logging/event pipelines to set the stage for future security/compliance requirements.

## Running Locally

### Prerequisites
- JDK 21
- Maven (`./mvnw` wrapper included)

### Starting the Server
```bash
./mvnw spring-boot:run
```
The server will start on `http://localhost:8080`.

### Local Endpoints
- **Health check:** `http://localhost:8080/actuator/health`
- **Ping test:** `http://localhost:8080/api/v1/ping`
- **Service info:** `http://localhost:8080/api/v1/info`

## Integrations
This service acts as a centralized boundary for external integrations. Clients handles external services.
Currently included integrations:
- `BuildiumClient`
- `GoogleSheetsClient`
- `SalesforceClient`

If adding a new integration boundary, define an interface and its implementation in the `client/` package, adhering to the standard wrapper approach. Keep configurations mapped into `application.yml`.

## Configuration
This service uses environment variables to overlay configurations mapped inside `src/main/resources/application.yml`. 

Key configuration properties:
```properties
BUILDIUM_BASE_URL=https://api.buildium.com
BUILDIUM_CLIENT_ID=<your-client-id>
BUILDIUM_CLIENT_SECRET=<your-client-secret>

GOOGLE_APPLICATION_CREDENTIALS=/path/to/service/account.json
SALESFORCE_ENABLED=true
```

## Running Tests
To run the automated test suite (JUnit 5 + Mockito):
```bash
./mvnw test
```

## Next Recommended Implementation Steps
1. **Flesh out Integration Clients:** Add actual implementation code (RestTemplate/WebClient calls) to the placeholder classes inside the `client/` package.
2. **Setup Global Security:** Depending on caller requirements (internal vs external), add basic API Key or OAuth2 resource server security via Spring Security.
3. **Database Migrations:** If job state or audit data needs persistence, introduce `Flyway` or `Liquibase` along with Spring Data JPA.
4. **Structured JSON Logging:** For easier ingestion into Datadog or ELK, swap the standard console Logback layout pattern for a JSON encoder (e.g., `LogstashTcpSocketAppender`).
