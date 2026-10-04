# Sarees E-Commerce Backend

Enterprise Spring Boot backend for the Sarees E-Commerce application.

## Stack

- Java 21
- Spring Boot 4.1.1
- Maven
- PostgreSQL, Spring Data JPA, Flyway
- Spring Security
- Redis
- Lombok

## Base package

`com.sarees.ecommerce`

## Run locally

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Profiles

| Profile   | Config file              |
|-----------|--------------------------|
| local     | application-local.yml    |
| dev       | application-dev.yml      |
| preprod   | application-preprod.yml  |
| prod      | application-prod.yml     |
| test      | application-test.yml     |

## Database migrations

Add Flyway scripts under `src/main/resources/db/migration/` (e.g. `V1__initial_schema.sql`).
