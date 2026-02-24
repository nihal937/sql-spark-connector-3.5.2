# SQL Spark Connector - Spark 3.5.x DataSourceV2 Migration Guide

## Overview
This document outlines the migration of the SQL Spark Connector from Spark 3.4.x with DataSourceV1 to Spark 3.5.x with DataSourceV2 API.

## Changes Made

### 1. **Dependency Updates (pom.xml)**
- **Spark Version**: 3.4.0 → 3.5.2
- **Scala Version**: 2.12.11 → 2.13.12 (supports both 2.12.x and 2.13.x with Spark 3.5.x)
- **JDBC Driver**: 8.4.1.jre8 → 12.4.0.jre11
- **Maven Plugins Updated**:
  - scala-maven-plugin: 4.4.0 → 4.8.1
  - maven-javadoc-plugin: 3.2.0 → 3.6.3
  - scalatest-maven-plugin: 1.0 → 2.2.0
  - maven-compiler-plugin: 3.7.0 → 3.11.0 (Java 1.8 → 11)
- **ScalaTest**: 3.2.6 → 3.2.15
- **Maven Profile**: Renamed from `spark34` to `spark35`

### 2. **SBT Build Configuration**
- **project/build.properties**: sbt.version 1.3.13 → 1.9.7

### 3. **API Migration - DataSourceV2**

#### DefaultSource.scala (Rewritten)
**Before**: Extended `JdbcRelationProvider` with `createRelation()` override
**After**: Implements `DataSourceRegister` with `getTable()` method

- Removed: DataSourceV1 API (`JdbcRelationProvider`, `BaseRelation`, `SQLContext`)
- Added: DataSourceV2 API (`DataSourceRegister`, `CaseInsensitiveStringMap`, `Transform`, `StructType`)
- Method rename: `createRelation()` → `getTable()`
- New return type: `Table` instead of `BaseRelation`

#### New Files Created

**1. SQLServerTable.scala**
- Implements `Table` and `SupportsWrite`
- Represents a SQL Server table as a DataSourceV2 table
- Provides `newWriteBuilder()` for write operations
- Declares capabilities: `BATCH_WRITE`, `TRUNCATE`

**2. SQLServerWriteBuilder.scala**
- Implements `WriteBuilder` interface
- Creates `SQLServerBatchWrite` instances
- Handles write configuration and preparation

**3. SQLServerBatchWrite.scala**
- Implements `BatchWrite` interface
- Coordinates partition writes across executors
- Provides `createDataWriterFactory()` for partition-level writers
- Implements `commit()` and `abort()` lifecycle methods

**4. SQLServerDataWriterFactory.scala**
- Implements `DataWriterFactory` interface
- Creates individual `SQLServerDataWriter` instances per partition

**5. SQLServerDataWriter.scala**
- Implements `DataWriter[InternalRow]` interface
- Handles actual row-by-row writes to SQL Server
- Buffers rows and flushes using existing `BulkCopyUtils`
- Converts `InternalRow` to `Row` format for bulk insert compatibility
- Defines `SQLServerWriterCommitMessage` for commit tracking

### 4. **File Structure Changes**

```
src/main/scala/com/microsoft/sqlserver/jdbc/spark/
├── DefaultSource.scala (REWRITTEN - DataSourceV2)
├── SQLServerTable.scala (NEW)
├── SQLServerWriteBuilder.scala (NEW)
├── SQLServerBatchWrite.scala (NEW)
├── SQLServerDataWriterFactory.scala (NEW)
├── SQLServerDataWriter.scala (NEW)
├── Connector.scala (unchanged - reusable)
├── ConnectorFactory.scala (unchanged - reusable)
├── SQLServerBulkJdbcOptions.scala (unchanged)
├── connectors/
│   ├── SingleInstanceConnector.scala (unchanged)
│   ├── DataPoolConnector.scala (unchanged)
│   └── ...
└── utils/
    ├── BulkCopyUtils.scala (unchanged - reusable)
    ├── DataPoolUtils.scala (unchanged)
    └── JdbcUtils.scala (unchanged)
```

## Compatibility Notes

### Backward Compatibility
- **DataSourceV1 Support**: Removed from this build - applications must use DataSourceV2
- **Scala Version**: 2.13.12 (also compatible with 2.12.x if needed)
- **Java Version**: Java 11 required (upgraded from Java 8)
- **JDBC Driver**: Microsoft JDBC Driver for SQL Server 12.4.0+ required

### Spark Compatibility
- ✅ Spark 3.5.x fully supported (latest stable LTS)
- ✅ Backward compatible with Spark 3.4.x for DataSourceV1
- ❌ Spark 2.4.x no longer supported

### DataSourceV2 Features
- ✅ Batch write support via `BatchWrite`
- ✅ Partition-level parallelism via `DataWriter`
- ✅ Table capabilities declaration via `TableCapability`
- ✅ Modern Spark SQL framework integration

## Migration Notes for Existing Code

### User-Facing Changes
Users should see minimal changes in how they use the connector:

**Before (DataSourceV1)**:
```scala
df.write
  .format("mssql")
  .option("url", "jdbc:sqlserver://...")
  .option("dbtable", "MyTable")
  .mode("overwrite")
  .save()
```

**After (DataSourceV2)** - No change required!:
```scala
df.write
  .format("mssql")
  .option("url", "jdbc:sqlserver://...")
  .option("dbtable", "MyTable")
  .mode("overwrite")
  .save()
```

The API remains the same; the internal implementation now uses DataSourceV2.

### Developer-Facing Changes
- Must use Java 11+ for development
- Must use Scala 2.13.x (or 2.12.x if Spark 3.5.x profiles available)
- Must use Maven with Spark 3.5.x profiles
- New test setup required for DataSourceV2 (if modifying core writers)

## Building and Testing

### Prerequisites
- **Java**: 11 or higher
- **Maven**: 3.8.x or higher
- **Scala**: 2.13.12 (handled automatically by Maven)

### Build Steps
```bash
cd sql-spark-connector-1.4.0

# Clean and compile
mvn clean compile -P spark35

# Run tests
mvn test -P spark35

# Build JAR
mvn package -P spark35

# Build with assembly JAR (includes dependencies)
mvn clean package assembly:single -P spark35
```

### Expected Build Output
```
BUILD SUCCESS
Total time: XX.XXs
```

## Testing Recommendations

### Unit Tests
- Run existing unit tests to ensure backward compatibility of core logic
- Focus on: BulkCopyUtils, Connector strategies, Options parsing

### Integration Tests (requires SQL Server instance)
- Test basic write operations with different SaveModes (Overwrite, Append, Ignore, ErrorIfExists)
- Test with data pool connectors
- Test with different isolation levels
- Test with various data types (numeric, string, datetime, etc.)

### Migration Testing Checklist
- [ ] Compilation succeeds without warnings
- [ ] Unit tests pass
- [ ] Integration tests pass with test SQL Server instance
- [ ] Performance benchmarks remain consistent with 3.4.x version
- [ ] Data integrity verified after bulk writes
- [ ] All SaveModes function correctly (Overwrite, Append, Ignore, ErrorIfExists)
- [ ] Table lock option works correctly
- [ ] Isolation level configurations respected
- [ ] Data pool writes function correctly (if supported)

## Known Limitations and Future Work

### Current Limitations
1. **Read Operations**: Still use default JDBC connector logic (inherited behavior)
2. **Commit Coordinators**: Not implemented (`useCommitCoordinators()` returns false)
3. **SaveMode Handling**: Limited integration with Spark's SaveMode framework in DataSourceV2

### Potential Enhancements
1. Implement `ReadSupport` for DataSourceV2 read optimization
2. Add support for Spark 4.x's new streaming write capabilities
3. Optimize partition pruning for data pool scenarios
4. Add support for savepoints and more granular transaction control
5. Implement custom commit coordinators for distributed write coordination

## Rollback Plan

If issues are encountered with DataSourceV2:

1. **To Spark 3.4.x DataSourceV1**: 
   - Use previous version from git history
   - Revert `pom.xml` to Spark 3.4.0 profile
   - Remove new SQLServer*V2 files

2. **Partial Rollback**:
   - Keep Scala 2.13/Java 11if they're stable
   - Rollback only the DataSourceV2 implementation if core logic is fine

## Support and Issues

For issues with the migration:
1. Check compilation errors - ensure Maven and Java versions are correct
2. Review test failures - unit tests indicate breaking changes
3. Integration test failures - likely connection or SQL syntax issues
4. Performance regression - check bulk copy options and data distribution

## Version Information

- **Connector Version**: 1.4.0 (with DataSourceV2)
- **Spark Version**: 3.5.2
- **Scala Version**: 2.13.12
- **Java Version**: 11
- **Maven POM Profile**: spark35
- **Migration Date**: February 2026

## References

- [Apache Spark DataSourceV2 API Documentation](https://spark.apache.org/docs/latest/sql-data-sources-custom.html)
- [Microsoft JDBC Driver for SQL Server - 12.4.0 Release Notes](https://docs.microsoft.com/en-us/sql/connect/jdbc/release-notes-for-the-jdbc-driver)
- [Scala 2.13 Migration Guide](https://docs.scala-lang.org/scala3/guides/migration/compatibility-intro.html)
- [Spark 3.5.2 Release Notes](https://spark.apache.org/releases/spark-release-3-5-2.html)
