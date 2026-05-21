# AGENTS

This file helps AI coding agents become productive quickly in this repository.

## Project Snapshot

- Stack: Java 21, Spring Boot 3.2.2, JavaFX 21, Maven
- Main launcher: src/main/java/com/library/pos/LibraryPosApplicationLauncher.java
- Default DB profile: SQLite (local file)
- Optional DB profile: MySQL via Spring profile

## Primary References (Read First)

- README and quick start: [README.md](README.md)
- Run notes and Java version constraints: [RUN.md](RUN.md)
- Detailed module and file documentation: [DOCUMENTATION.md](DOCUMENTATION.md)
- Build and dependency configuration: [pom.xml](pom.xml)
- MySQL local container setup: [docker-compose.yml](docker-compose.yml)
- SQL schema and migration baseline: [library_pos.sql](library_pos.sql)

## Build, Run, and Package

Use the Maven wrapper on Windows unless you have a specific reason not to.

- JavaFX run: `mvnw.cmd javafx:run`
- Spring Boot run: `mvnw.cmd spring-boot:run`
- Clean package: `mvnw.cmd clean package -DskipTests`
- EXE packaging script: `create_exe.bat`
- Legacy run script: `run.bat`

## Architecture Map

- Controllers (UI flow): src/main/java/com/library/pos/controller
- Services (business logic): src/main/java/com/library/pos/service
- Repositories (data access): src/main/java/com/library/pos/repository
- Models/entities: src/main/java/com/library/pos/model
- Shared helpers: src/main/java/com/library/pos/util
- UI resources: src/main/resources (FXML, CSS, i18n, properties)

## Conventions Agents Should Follow

- Keep controller logic thin and move business rules to service classes.
- Preserve Spring annotations and transactional boundaries in service changes.
- Keep persistence changes aligned with existing JPA entity/repository patterns.
- Prefer editing source files under src and SQL inputs in repository root.
- Update documentation links only when behavior or workflow changes.

## Known Pitfalls

- Java 21 is required for current Lombok and JavaFX compatibility.
- This repo has many generated and backup artifacts; do not treat them as source-of-truth.
- Avoid editing generated outputs and packaging leftovers in backups and target.

## Do Not Edit By Default

- backups/**
- target/**
- build logs and crash logs at repository root unless task explicitly asks for log analysis

## Database Notes

- Default profile uses SQLite settings from src/main/resources/application.properties.
- MySQL profile uses src/main/resources/application-mysql.properties.
- Local MySQL can be started with Docker Compose, then use the mysql Spring profile.

## Validation Guidance

When changing Java code:

1. Compile first with Maven wrapper.
2. Run the most relevant launcher flow.
3. If DB-related, verify both SQL and JPA assumptions.
4. Keep changes scoped; avoid broad refactors unless requested.

## Suggested Next Customizations

If this project grows, consider adding:

- File-scoped instructions in .github/instructions for SQL files vs Java files.
- A custom prompt for repetitive packaging/release steps.
- A project skill for receipt-printing related changes and validation.
