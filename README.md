# TraceRoute

College end-semester backend foundation. Java 21, Spring Boot 3.5.16, Maven, MySQL 8; root package `com.traceroute`.

- [Setup and demonstration](docs/SETUP.md)
- [API contract](docs/API_CONTRACT.md)
- [Entity and teammate contract](docs/ENTITY_CONTRACT.md)
- [Reference schema](database/schema.sql)

From `backend`, run `./mvnw clean verify` (Windows: `.\mvnw.cmd clean verify`). Tests use isolated in-memory H2 and require no MySQL credentials. To run the application, create the MySQL database and set environment variables as described in SETUP, then run `.\mvnw.cmd spring-boot:run`.

Implemented: student registration, stateless login, current user, admin user listing, database-backed HTTP Basic, BCrypt, validation, JSON authentication errors, CORS, and authentication integration tests. Incident/graph business logic and the React UI belong to teammates and are not implemented.
