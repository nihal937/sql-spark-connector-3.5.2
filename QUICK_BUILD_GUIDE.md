# SQL Spark Connector 4.2.x - Build Guide

## Quick Start (TL;DR)

### Prerequisites
- Windows 10/11
- Administrator access (for installation)
- Internet connection

### Installation Steps

#### 1. Install Java 11
Download and install from: https://www.oracle.com/java/technologies/downloads/#java11
- Choose "JDK 11" → Windows x64 Installer
- Run the installer
- Accept all defaults
- **Close and restart your command prompt/PowerShell**

#### 2. Install Maven
Download and install from: https://maven.apache.org/download.cgi
- Download "Binary zip archive" (apache-maven-3.9.x-bin.zip)
- Extract to: `C:\tools\apache-maven-3.9.x`
- Add environment variable `MAVEN_HOME` = `C:\tools\apache-maven-3.9.x`
- Add to PATH: `C:\tools\apache-maven-3.9.x\bin`
- **Close and restart your command prompt/PowerShell**

#### 3. Verify Installation
Open a **new** Command Prompt and run:
```batch
java -version
mvn --version
```

Both should show version info (Java 11+, Maven 3.8+)

### Build the Project

#### Method 1: Using the batch script (Easiest)
```batch
cd C:\Users\user\Downloads\sql-spark-connector-1.4.0\sql-spark-connector-1.4.0
build.bat
```

#### Method 2: Using Maven directly
```batch
cd C:\Users\user\Downloads\sql-spark-connector-1.4.0\sql-spark-connector-1.4.0
mvn clean package -P spark42
```

### Output
The built JAR files will be in the `target\` folder:
- `spark-mssql-connector-1.4.0.jar` - Use this for Spark jobs
- `spark-mssql-connector-1.4.0-sources.jar` - Source code
- `spark-mssql-connector-1.4.0-javadoc.jar` - API documentation

---

## Detailed Instructions

### Windows 10/11 Environment Setup

#### Step 1: Install Java 11

**Option A: Manual Installation (Recommended)**

1. Visit: https://www.oracle.com/java/technologies/downloads/#java11
2. Click "Download" next to "Windows x64 Installer"
3. Accept the license and download the `.exe` file
4. Run the installer
5. Accept all default settings during installation
6. The installer will automatically add Java to your PATH
7. **Important**: Close and reopen any open Command Prompt or PowerShell windows

**Option B: Using Windows Package Manager (if available)**

Open Command Prompt as Administrator:
```batch
winget install Oracle.JDK.11
```

**Verify Installation**

Open a new Command Prompt and run:
```batch
java -version
```

Expected output:
```
openjdk version "11.0.x" 2024-... 
OpenJDK Runtime Environment ...
```

#### Step 2: Install Maven

1. Visit: https://maven.apache.org/download.cgi
2. Under "Files", click the link for "Binary zip archive" (e.g., `apache-maven-3.9.x-bin.zip`)
3. Extract the ZIP file to a location like: `C:\tools\apache-maven-3.9.x`
4. Set up environment variables:
   - Right-click **This PC** → **Properties** → **Advanced system settings** → **Environment Variables**
   - Click **New** under "System variables"
   - Variable name: `MAVEN_HOME`
   - Variable value: `C:\tools\apache-maven-3.9.x`
   - Click **OK**
   - Find the "Path" variable in the list, click **Edit**
   - Click **New** and add: `C:\tools\apache-maven-3.9.x\bin`
   - Click **OK** on all dialogs
5. **Close and reopen any open Command Prompt or PowerShell windows**

**Verify Installation**

Open a new Command Prompt and run:
```batch
mvn --version
```

Expected output:
```
Apache Maven 3.9.x ...
Java version: 11.0.x ...
```

### Building the Project

#### Using the provided batch script (Easiest)

1. Open Command Prompt
2. Navigate to the project:
   ```batch
   cd "C:\Users\user\Downloads\sql-spark-connector-1.4.0\sql-spark-connector-1.4.0"
   ```
3. Run the build script:
   ```batch
   build.bat
   ```
4. Wait for the build to complete (first build takes 2-5 minutes)
5. Check for "BUILD SUCCESS" message at the end

#### Using Maven from Command Prompt

```batch
cd "C:\Users\user\Downloads\sql-spark-connector-1.4.0\sql-spark-connector-1.4.0"
mvn clean package -P spark42
```

#### Using Maven from PowerShell

```powershell
cd "C:\Users\user\Downloads\sql-spark-connector-1.4.0\sql-spark-connector-1.4.0"
mvn clean package -P spark42
```

### Build Options

| Command | Purpose | Time |
|---------|---------|------|
| `mvn clean compile -P spark42` | Compile only (no JAR) | ~1 min |
| `mvn clean package -P spark42` | Compile + Package JAR | ~2-3 min |
| `mvn clean package test -P spark42` | Compile + Package + Tests | ~5-10 min |

### Build Output

After a successful build, you'll see:
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXs
```

The JAR files are created in the `target\` directory:

```
target\
  spark-mssql-connector-1.4.0.jar          ← Use this file
  spark-mssql-connector-1.4.0-sources.jar
  spark-mssql-connector-1.4.0-javadoc.jar
```

---

## Troubleshooting

### Problem: `java' is not recognized`

**Cause**: Java not installed or not in PATH

**Solution**:
1. Install Java from: https://www.oracle.com/java/technologies/downloads/#java11
2. **Close all open Command Prompt/PowerShell windows**
3. Open a **new** Command Prompt and try again

### Problem: `mvn' is not recognized`

**Cause**: Maven not installed or not in PATH

**Solution**:
1. Install Maven from: https://maven.apache.org/download.cgi
2. Extract to: `C:\tools\apache-maven-3.9.x`
3. Set environment variable:
   - Search for "Environment Variables" in Windows search
   - Edit system environment variables
   - Add `MAVEN_HOME` pointing to the Maven directory
   - Add `C:\tools\apache-maven-3.9.x\bin` to PATH
4. **Close all open Command Prompt/PowerShell windows**
5. Open a **new** Command Prompt and try again

### Problem: `UnsupportedClassVersionError` or similar Java error

**Cause**: Java version is too old (older than Java 11)

**Solution**:
```batch
java -version
```
- If version shows less than 11, uninstall and install Java 11 from the link above
- If version is correct, make sure Maven is using the right Java:
  ```batch
  mvn --version
  ```
  Should show Java 11+

### Problem: Build downloads very slowly or hangs

**Cause**: Maven is downloading dependencies (normal on first build)

**Solution**:
- First build downloads ~500 MB of dependencies
- This can take 5-15 minutes depending on internet speed
- Be patient, it only happens once
- Subsequent builds are much faster

### Problem: "Compilation errors" in build output

**Cause**: Missing dependencies or incorrect Spark profile

**Solution**:
1. Make sure you include the profile: `-P spark42`
2. Try cleaning: `mvn clean`
3. Force Maven to update: `mvn -U clean package -P spark42`
4. Delete local Maven cache:
   ```batch
   rmdir /s /q "%USERPROFILE%\.m2\repository"
   ```
   Then run build again

### Problem: Build failed with connection error

**Cause**: Network issues downloading dependencies

**Solution**:
1. Check internet connection
2. Try again in a few minutes
3. If issue persists, try:
   ```batch
   mvn clean package -DofflineMode=false -P spark42
   ```

---

## Using the Built JAR

After successful build, the JAR is ready to use in Spark:

### With Spark Python
```python
spark.sparkContext.addJar("path/to/spark-mssql-connector-1.4.0.jar")

df.write \
  .format("mssql") \
  .option("url", "jdbc:sqlserver://localhost:1433") \
  .option("user", "sa") \
  .option("password", "YourPassword") \
  .option("dbtable", "dbo.MyTable") \
  .mode("overwrite") \
  .save()
```

### With Spark Submit
```bash
spark-submit \
  --jars spark-mssql-connector-1.4.0.jar \
  --class MyApp \
  my-application.jar
```

### With Databricks
1. Upload the JAR to your Databricks workspace
2. Attach to cluster in Libraries section
3. Use in notebooks same as above

---

## Additional Documentation

- **[DATASOURCEV2_MIGRATION.md](DATASOURCEV2_MIGRATION.md)** - Technical details of Spark 4.2.x migration
- **[BUILD_AND_TEST.md](BUILD_AND_TEST.md)** - Advanced build options and testing
- **[SETUP_AND_BUILD.md](SETUP_AND_BUILD.md)** - Detailed environment setup guide
- **[README.md](README.md)** - Original connector documentation

---

## Next Steps

1. ✓ Install Java 11 and Maven
2. ✓ Run `build.bat` or `mvn clean package -P spark42`
3. → Review [DATASOURCEV2_MIGRATION.md](DATASOURCEV2_MIGRATION.md) for what changed
4. → Use the JAR in your Spark environment
5. → Run tests with your SQL Server instance (optional)

---

## Support

If you still have issues:
1. Verify Java and Maven are working:
   ```batch
   java -version
   mvn --version
   ```
2. Check that you're in the correct directory
3. Review the error message in the build output
4. Check the troubleshooting section above
