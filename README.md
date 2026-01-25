# How to Run the Library POS System

## Prerequisites
- **Java Development Kit (JDK) 21** or higher.
- A Java IDE (IntelliJ IDEA, Eclipse, or VS Code).

## Option 1: Run via IDE (Recommended)
Since Maven is not in your system PATH, the easiest way is to use your IDE.

1.  Open the project folder (`C:\Users\user\Library-Dr-Khaled`) in your IDE.
2.  Wait for the IDE to import dependencies from `pom.xml`.
3.  Navigate to:
    `src/main/java/com/library/pos/LibraryPosApplicationLauncher.java`
4.  Right-click the file (or the `main` method) and select **Run 'LibraryPosApplicationLauncher'**.

## Option 2: Run via Terminal (If Maven is configured)
If you configure Maven in your PATH later, you can run:

```powershell
mvn clean javafx:run
```
OR
```powershell
mvn spring-boot:run
```

## Option 3: Run with Docker MySQL (Shareable Setup)
This uses a local MySQL container so you can share the same setup with a friend.

1. Start MySQL:
```powershell
docker compose up -d
```

2. Run the app with the MySQL profile:
```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"
mvn spring-boot:run
```

If you run from the IDE, set the environment variable `SPRING_PROFILES_ACTIVE=mysql` in the Run Configuration.

## Troubleshooting
- **"Symbol not found" errors**: Make sure your IDE has finished indexing and downloading Maven dependencies.
- **Port 8080 already in use**: Open `src/main/resources/application.properties` and change `server.port` to something else (e.g., 8081).
