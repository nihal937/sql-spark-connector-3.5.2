# Build Setup and Instructions for SQL Spark Connector

## Step 1: Install Java 11+

Java is required to build the project. You need Java 11 or higher.

### Option A: Using Windows Package Manager (Recommended)
If you have Windows 11 or newer with `winget`:

```powershell
# Open PowerShell as Administrator and run:
winget install Oracle.JDK.11
```

### Option B: Download and Install Manually
1. Go to [Oracle Java Downloads](https://www.oracle.com/java/technologies/downloads/#java11)
2. Download **JDK 11** for Windows (x64)
3. Run the installer and follow the installation wizard
4. Default installation location: `C:\Program Files\Java\jdk-11.x.x`

### Option C: Using Chocolatey
If you have Chocolatey installed:
```powershell
choco install openjdk11
```

### Verify Java Installation
After installation, open a **new** PowerShell window and run:
```powershell
java -version
```

Expected output:
```
openjdk version "11.x.x" ...
```

## Step 2: Install Maven

Maven is the build tool needed to compile and package the connector.

### Option A: Using Windows Package Manager (Recommended)
```powershell
winget install Apache.Maven
```

### Option B: Download and Install Manually
1. Go to [Maven Downloads](https://maven.apache.org/download.cgi)
2. Download **Binary zip archive** (maven-3.9.x-bin.zip)
3. Extract to a clean location, e.g., `C:\tools\apache-maven-3.9.x`
4. Set environment variable:
   - Open **System Properties** → **Environment Variables**
   - Create new **User variable**: `MAVEN_HOME = C:\tools\apache-maven-3.9.x`
   - Edit **Path** system variable, add: `C:\tools\apache-maven-3.9.x\bin`

### Option C: Using Chocolatey
```powershell
choco install maven
```

### Verify Maven Installation
Open a **new** PowerShell window and run:
```powershell
mvn --version
```

Expected output:
```
Apache Maven 3.9.x ...
Java version: 11.x.x ...
```

## Step 3: Build the Project

Once Java and Maven are installed, open a PowerShell window in the project directory:

```powershell
# Navigate to project directory
cd "C:\Users\user\Downloads\sql-spark-connector-1.4.0\sql-spark-connector-1.4.0"

# Clean and compile
mvn clean compile -P spark42
```

### Build Options

**Option 1: Compile Only** (fast, no JAR)
```powershell
mvn clean compile -P spark42
```

**Option 2: Compile + Package** (creates JAR)
```powershell
mvn clean package -P spark42
```

**Option 3: Compile + Package + Tests** (most thorough)
```powershell
mvn clean package test -P spark42
```

**Option 4: Build with All Dependencies (Fat JAR)**
```powershell
mvn clean package assembly:single -P spark42
```

## Expected Build Output

When the build succeeds, you'll see:

```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXs
[INFO] Finished at: YYYY-MM-DDTHH:MM:SS
```

### Output Artifacts

The built JARs will be in the `target/` folder:

- `spark-mssql-connector-1.4.0.jar` - Main connector JAR
- `spark-mssql-connector-1.4.0-sources.jar` - Source code JAR
- `spark-mssql-connector-1.4.0-javadoc.jar` - Javadoc JAR
- `spark-mssql-connector-1.4.0-jar-with-dependencies.jar` - Fat JAR (if using assembly:single)

## Troubleshooting

### Issue: `java: The term 'java' is not recognized`
**Cause**: Java not in PATH after installation
**Solution**: 
1. Verify Java installed: `ls "C:\Program Files\Java\"`
2. Close and reopen PowerShell (must reload PATH)
3. Or manually set PATH:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-11.x.x"
   $env:Path += ";$env:JAVA_HOME\bin"
   java -version
   ```

### Issue: `mvn: The term 'mvn' is not recognized`
**Cause**: Maven not in PATH
**Solution**: Same as Java above - close/reopen PowerShell or manually set:
   ```powershell
   $env:MAVEN_HOME = "C:\tools\apache-maven-3.9.x"
   $env:Path += ";$env:MAVEN_HOME\bin"
   mvn --version
   ```

### Issue: `UnsupportedClassVersionError`
**Cause**: Java version too old (less than 11)
**Solution**: 
```powershell
java -version  # Should show version 11+
# If not, reinstall Java 11 or higher
```

### Issue: Compilation errors mentioning Spark classes
**Cause**: Missing or incorrect profile
**Solution**: Ensure `-P spark42` is included in command:
```powershell
mvn clean compile -P spark42
```

### Issue: `Could not find artifact org.apache.spark:spark-sql_2.13:4.2.0`
**Cause**: Network issues or local Maven cache corrupted
**Solution**:
```powershell
# Force Maven to re-download dependencies
mvn -U clean compile -P spark42

# Or clear local cache:
rmdir -Force -Recurse "$env:USERPROFILE\.m2\repository"
mvn clean compile -P spark42
```

### Issue: Build takes very long time on first run
**Note**: This is normal! Maven downloads 500+ MB of dependencies on first build.
- Be patient, it should complete in 2-5 minutes
- Subsequent builds are much faster

## Verifying the Build

After successful build, verify the JAR contains DataSourceV2 classes:

```powershell
# List classes in JAR
jar tf target\spark-mssql-connector-1.4.0.jar | Select-String "SQLServer.*\.class"
```

Expected output should include:
```
com/microsoft/sqlserver/jdbc/spark/DefaultSource.class
com/microsoft/sqlserver/jdbc/spark/SQLServerTable.class
com/microsoft/sqlserver/jdbc/spark/SQLServerWriteBuilder.class
com/microsoft/sqlserver/jdbc/spark/SQLServerBatchWrite.class
com/microsoft/sqlserver/jdbc/spark/SQLServerDataWriter.class
com/microsoft/sqlserver/jdbc/spark/SQLServerDataWriterFactory.class
```

## Running Tests (Optional)

To verify everything works with tests:

```powershell
# Run all tests
mvn test -P spark42

# Run specific test class
mvn test -P spark42 -Dtest=ConnectorTestUtils

# Run without tests (faster)
mvn package -DskipTests -P spark42
```

## Next Steps After Build

1. **Use the JAR**: The `target/spark-mssql-connector-1.4.0.jar` is ready to use
2. **Review Changes**: Read [DATASOURCEV2_MIGRATION.md](DATASOURCEV2_MIGRATION.md)
3. **Deploy**: Upload to your Spark cluster or development environment
4. **Test with SQL Server**: Run integration tests with your SQL Server instance

## Quick Reference - Command Summary

```powershell
# Installation (run once)
winget install Oracle.JDK.11
winget install Apache.Maven

# Build project (run from project directory)
cd "C:\Users\user\Downloads\sql-spark-connector-1.4.0\sql-spark-connector-1.4.0"
mvn clean package -P spark42

# Verify
jar tf target\spark-mssql-connector-1.4.0.jar | grep DefaultSource
```

## Support

If you encounter issues:
1. Verify Java: `java -version` (must be 11+)
2. Verify Maven: `mvn --version`
3. Check project POM: Look for profile named `spark42`
4. Review detailed error: `mvn clean compile -P spark42 -X` (verbose output)
