# File-by-File Documentation

This document explains the purpose and typical usage of every file in this project.

## Root
- `README.md` — Quick start + project overview.
- `pom.xml` — Maven build config (dependencies, plugins, Java version).
- `docker-compose.yml` — Defines a local MySQL container for development or shared DB.
- `library_pos.sql` — Database schema and seed data for MySQL/MariaDB.
- `mvnw`, `mvnw.cmd`, `.mvn/` — Maven wrapper (run Maven without installing it).
- `target/` — Build output (generated; safe to delete).
- `hs_err_pid*.log` — JVM crash logs (generated; for debugging crashes).
- `.vscode/` — IDE settings.
- `.git/` — Git metadata (ignore).

## Java Source (`src/main/java/com/library/pos`)
- `LibraryPosApplicationLauncher.java` — Spring Boot entry point; launches JavaFX app.
- `LibraryPosApplication.java` — JavaFX Application: loads theme, sets locale, loads FXML, shows main stage.

### Controllers (`src/main/java/com/library/pos/controller`)
- `LoginController.java` — Handles login UI actions and authentication.
- `DashboardController.java` — Controls dashboard view (summary, navigation, main actions).
- `ProductsController.java` — Product list/add/edit/delete UI.
- `WorkersController.java` — Worker management UI (add/list workers).

### Services (`src/main/java/com/library/pos/service`)
- `AuthService.java` — Authentication logic and current user state.
- `ProductService.java` — Product business logic and DB operations.
- `SaleService.java` — Sales logic (create/update sales, totals, status).
- `UserService.java` — User/worker business logic and DB operations.

### Repositories (`src/main/java/com/library/pos/repository`)
- `ProductRepository.java` — Database access for products.
- `SaleRepository.java` — Database access for sales.
- `UserRepository.java` — Database access for users/workers.

### Models (`src/main/java/com/library/pos/model`)
- `Product.java` — Product entity/model.
- `Sale.java` — Sale entity/model.
- `User.java` — User/worker entity/model.
- `Role.java` — User roles enum (OWNER/WORKER).
- `SaleStatus.java` — Sale status enum.

## Resources (`src/main/resources`)
- `application.properties` — Default Spring Boot configuration.
- `application-mysql.properties` — MySQL profile config (use with `SPRING_PROFILES_ACTIVE=mysql`).
- `messages.properties` — Default UI strings.
- `messages_ar.properties` — Arabic UI strings (default locale).
- `css/style.css` — App styling.
- `fxml/login.fxml` — Login view.
- `fxml/dashboard.fxml` — Dashboard view.
- `fxml/products.fxml` — Products view.
- `fxml/workers.fxml` — Workers view.
