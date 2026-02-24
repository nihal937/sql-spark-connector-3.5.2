# Quick Start - Build and Test Setup

## Environment Setup

### 1. Install Prerequisites

#### Windows (PowerShell)
```powershell
# Install Java 11 (if not already installed)
# Download from: https://www.oracle.com/java/technologies/downloads/#java11
# Or use Windows Package Manager:
winget install Oracle.JavaRuntimeEnvironment

# Install Maven (if not already installed)
winget install Apache.Maven
# Or manually: https://maven.apache.org/download.cgi

# Verify installations
java -version          # Should show Java 11+
mvn -version           # Should show Maven 3.8.x+
```

#### macOS/Linux
```bash
# Install Java 11 (using Homebrew on macOS)
brew install openjdk@11
# Or download from Oracle

# Install Maven
brew install maven
# Or: sudo apt-get install maven (Linux)

# Verify
java -version
mvn -version
```

### 2. Add Maven to PATH (if needed)

**Windows - PowerShell** (if Maven not in PATH):
```powershell
$env:Path += ";C:\Program Files\Apache\maven\bin"
# Add permanently by editing Environment Variables
```

**macOS/Linux**:
```bash
export PATH="/usr/local/opt/maven/bin:$PATH"
# Add to ~/.bash_profile or ~/.zshrc for persistence
```

### 3. Clone or Navigate to Project

```bash
cd sql-spark-connector-1.4.0
```

## Building the Project

### Clean Build
```bash
mvn clean compile -P spark42
```

**Output**: Should complete with `BUILD SUCCESS`

### Build with Tests
```bash
mvn clean package -P spark42
```

**Output**: Creates:
- `target/spark-mssql-connector-1.4.0.jar` (main library)

### Build with All Dependencies (Assembly JAR)
```bash
mvn clean package assembly:single -P spark42
```

**Output**: Creates:
- `target/spark-mssql-connector-1.4.0-jar-with-dependencies.jar` (all deps included)

## Running Tests

### Run All Tests
```bash
mvn test -P spark42
```

### Run Specific Test
```bash
mvn test -P spark42 -Dtest=ClassName
```

### Run Tests with Detailed Output
```bash
mvn test -P spark42 -X
```

## Troubleshooting

### Issue: "mvn command not found"
**Solution**: 
1. Install Maven properly (see Environment Setup)
2. Add Maven bin folder to system PATH
3. Restart terminal/PowerShell

### Issue: "java.lang.UnsupportedClassVersionError"
**Solution**: Java version mismatch
```bash
# Check Java version
java -version

# Should show "11." or higher
# If not, update JAVA_HOME environment variable to point to Java 11+
```

### Issue: "Could not find artifact org.apache.spark:spark-sql_2.13:4.2.0"
**Solution**: Maven dependencies not downloading
```bash
# Force update dependencies
mvn clean dependency:purge-local-repository compile

# Or manually clear and refresh:
mvn -U clean compile
```

### Issue: Compilation Errors in New DataSourceV2 Files
**Solution**: Check Spark API compatibility
1. Ensure Maven command includes `-P spark42` profile
2. Verify pom.xml has Spark 4.2.0 version
3. Check that Scala version is 2.13.12

## Verifying Installation

After successful build, verify the JAR was created:

```bash
# List created artifacts
ls -la target/spark-mssql-connector-*.jar

# Check JAR contents (should include DataSourceV2 classes)
jar tf target/spark-mssql-connector-1.4.0.jar | grep "SQLServer"
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

## Using the Built JAR

### With Spark Standalone
```scala
spark.jars("/path/to/spark-mssql-connector-1.4.0.jar")

df.write
  .format("mssql")
  .option("url", "jdbc:sqlserver://localhost:1433")
  .option("user", "sa")
  .option("password", "YourPassword123")
  .option("dbtable", "dbo.MyTable")
  .mode("overwrite")
  .save()
```

### With Databricks
Upload JAR to Databricks and attach to cluster, then use same Spark code above.

### With Spark Submit
```bash
spark-submit \
  --jars spark-mssql-connector-1.4.0-jar-with-dependencies.jar \
  --class MyApp \
  my-app.jar \
  args...
```

## Next Steps

1. **Review Changes**: Read [DATASOURCEV2_MIGRATION.md](DATASOURCEV2_MIGRATION.md)
2. **Run Tests**: Execute `mvn test -P spark42` to verify functionality
3. **Deploy**: Use the built JAR in your Spark environment
4. **Migrate Apps**: Update your applications to use the new JAR (API unchanged for users)

## Documentation

- **Migration Guide**: [DATASOURCEV2_MIGRATION.md](DATASOURCEV2_MIGRATION.md)
- **Original README**: [README.md](README.md)
- **Changelist**: [CHANGELIST.md](CHANGELIST.md)

## Support

For build issues or questions:
1. Check Maven installation: `mvn -version`
2. Check Java installation: `java -version`
3. Check pom.xml for correct profile: should be `spark42`
4. Review compiler output for specific error messages
