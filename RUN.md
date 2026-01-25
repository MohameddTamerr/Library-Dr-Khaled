# How to Run the Library POS Application

## Problem Identified

Your system has **Java 24** installed, but this project requires **Java 21** because:
- Lombok 1.18.34 (the latest stable version) doesn't support Java 24 yet
- The Spring Boot version (3.2.2) is configured for Java 21

## Solution Options

### Option 1: Install Java 21 (Recommended)

1. Download and install **Java 21 JDK** from:
   - [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21)
   - OR [Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21)

2. After installation, set JAVA_HOME to Java 21:
   ```powershell
   # Find where Java 21 is installed (usually C:\Program Files\Java\jdk-21)
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
   $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
   ```

3. Verify Java version:
   ```powershell
   java -version
   # Should show: java version "21.x.x"
   ```

4. Run the application:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

### Option 2: Use the Provided Run Script

I've created a `run.bat` script that will help you run the application with the correct Java version.

```powershell
.\run.bat
```

## Memory Configuration

The application is now configured with reduced memory settings to prevent out-of-memory errors:
- Initial heap: 256MB (`-Xms256m`)
- Maximum heap: 512MB (`-Xmx512m`)

These settings are in `pom.xml` and will be used automatically.

## What Was Fixed

1. ✅ Added JVM memory arguments to prevent memory allocation failures
2. ✅ Added JavaFX Maven plugin for proper JavaFX application launching
3. ✅ Configured Lombok 1.18.34 for Java 21 compatibility
4. ⚠️ **You need to install Java 21** to compile and run the application

## Current Error

If you try to run with Java 24, you'll see compilation errors like:
```
cannot find symbol: method getQuantity()
cannot find symbol: method getCost()
```

These occur because Lombok can't generate getters/setters on Java 24.
