/**
* Copyright 2020 and onwards Microsoft Corporation
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.microsoft.sqlserver.jdbc.spark

import com.microsoft.sqlserver.jdbc.spark.BulkCopyUtils.{defaultColMetadataMap, getEmptyResultSet, savePartition}
import com.microsoft.sqlserver.jdbc.spark.utils.JdbcUtils.createConnection
import org.apache.spark.internal.Logging
import org.apache.spark.sql.Row
import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.connector.write.DataWriter
import org.apache.spark.sql.connector.write.WriterCommitMessage
import org.apache.spark.sql.types._
import org.apache.spark.sql.util.CaseInsensitiveStringMap
import scala.collection.mutable.ArrayBuffer
import java.sql.Connection

/**
 * SQLServerDataWriter handles the actual write of a single partition to SQL Server.
 * It buffers rows and writes them using the SQL Server bulk copy API.
 * Automatically truncates table for SaveMode.Overwrite (partition 0 only).
 */
class SQLServerDataWriter(val partitionId: Int,
                          val taskId: Long,
                          val options: CaseInsensitiveStringMap,
                          val schema: StructType,
                          val saveMode: SaveMode = SaveMode.ErrorIfExists) extends DataWriter[InternalRow] with Logging {

  private val buffer = new ArrayBuffer[Row]()
  private val BUFFER_SIZE = 10000 // Batch size for writes
  private var connection: Connection = _
  private var colMetadata: Array[ColumnMetadata] = _
  private var totalRows = 0L
  private var isTruncated = false  // Track if table has been truncated
  private val opts = new SQLServerBulkJdbcOptions(convertCaseInsensitiveMapToScala(options))

  logDebug(s"SQLServerDataWriter initialized for partition $partitionId, task $taskId, saveMode=$saveMode")

  /**
   * write adds a row to the buffer and flushes when buffer reaches limit
   */
  override def write(row: InternalRow): Unit = {
    buffer += internalRowToRow(row)
    totalRows += 1
    if (buffer.size >= BUFFER_SIZE) {
      flushBuffer()
    }
  }

  /**
   * commit flushes remaining rows and returns commit message
   */
  override def commit(): WriterCommitMessage = {
    try {
      flushBuffer()
      logDebug(s"Partition $partitionId successfully committed $totalRows rows")
      SQLServerWriterCommitMessage(partitionId, totalRows)
    } finally {
      close()
    }
  }

  /**
   * abort closes the connection and discards buffered data on write failure
   */
  override def abort(): Unit = {
    try {
      buffer.clear()
      logDebug(s"Partition $partitionId aborted, discarded $totalRows rows")
    } finally {
      close()
    }
  }

  /**
   * close releases database connection resources
   */
  override def close(): Unit = {
    if (connection != null) {
      try {
        connection.close()
        logDebug(s"Connection closed for partition $partitionId")
      } catch {
        case e: Exception => logError(s"Error closing connection: ${e.getMessage}")
      }
    }
  }

  /**
   * flushBuffer writes buffered rows to SQL Server using bulk copy
   */
  private def flushBuffer(): Unit = {
    if (buffer.isEmpty) return

    try {
      if (connection == null) {
        connection = createConnection(opts)
        
        // Create table if it doesn't exist (only partition 0 to avoid race conditions)
        if (partitionId == 0) {
          if (!tableExists(connection, opts.dbtable)) {
            createTableFromSchema(connection, opts.dbtable, schema)
            logInfo(s"Created new table ${opts.dbtable} from DataFrame schema")
          }
        }
        
        // Get column metadata from the table
        val emptyRs = getEmptyResultSet(connection, opts.dbtable)
        colMetadata = defaultColMetadataMap(emptyRs.getMetaData())
        
        // Auto-truncate for Overwrite mode on first write
        // Only partition 0 truncates to avoid race conditions
        if (partitionId == 0 && !isTruncated && saveMode == SaveMode.Overwrite) {
          truncateTable(connection, opts.dbtable)
          isTruncated = true
          logInfo(s"Auto-truncated table ${opts.dbtable} for SaveMode.Overwrite")
        }
      }

      logDebug(s"Flushing ${buffer.size} rows from partition $partitionId")
      val rows = buffer.toList
      buffer.clear()

      // Use existing bulk copy infrastructure
      savePartition(rows.iterator, opts.dbtable, colMetadata, opts)
    } catch {
      case e: Exception =>
        logError(s"Error flushing buffer for partition $partitionId: ${e.getMessage}", e)
        throw e
    }
  }

  /**
   * Truncate the table before first write (for SaveMode.Overwrite)
   */
  private def truncateTable(conn: Connection, tableName: String): Unit = {
    try {
      val truncateQuery = s"TRUNCATE TABLE [$tableName]"
      val stmt = conn.createStatement()
      stmt.execute(truncateQuery)
      stmt.close()
      logInfo(s"Truncated table $tableName for partition $partitionId")
    } catch {
      case e: Exception =>
        logError(s"Error truncating table $tableName: ${e.getMessage}", e)
        throw new RuntimeException(s"Failed to truncate table: ${e.getMessage}", e)
    }
  }

  /**
   * Check if a table exists in SQL Server
   */
  private def tableExists(conn: Connection, tableName: String): Boolean = {
    try {
      val query = s"""
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES 
        WHERE TABLE_NAME = ? AND TABLE_SCHEMA = 'dbo'
      """
      val pstmt = conn.prepareStatement(query)
      pstmt.setString(1, tableName)
      val rs = pstmt.executeQuery()
      val exists = rs.next()
      rs.close()
      pstmt.close()
      exists
    } catch {
      case e: Exception =>
        logWarning(s"Error checking if table $tableName exists: ${e.getMessage}")
        false
    }
  }

  /**
   * Create table based on DataFrame schema
   */
  private def createTableFromSchema(conn: Connection, tableName: String, dfSchema: StructType): Unit = {
    try {
      val columnDefs = dfSchema.fields.map { field =>
        val sqlType = sparkTypeToSqlServerType(field.dataType, field.nullable)
        s"[${field.name}] $sqlType"
      }.mkString(",\n    ")

      val createTableQuery = s"""
        CREATE TABLE [$tableName] (
          $columnDefs
        )
      """

      logDebug(s"Creating table with query:\n$createTableQuery")
      val stmt = conn.createStatement()
      stmt.execute(createTableQuery)
      stmt.close()
      logInfo(s"Successfully created table $tableName")
    } catch {
      case e: Exception =>
        logError(s"Error creating table $tableName: ${e.getMessage}", e)
        throw new RuntimeException(s"Failed to create table: ${e.getMessage}", e)
    }
  }

  /**
   * Map Spark DataType to SQL Server column type
   */
  private def sparkTypeToSqlServerType(dataType: DataType, nullable: Boolean): String = {
    val baseType = dataType match {
      case ByteType => "TINYINT"
      case ShortType => "SMALLINT"
      case IntegerType => "INT"
      case LongType => "BIGINT"
      case FloatType => "REAL"
      case DoubleType => "FLOAT"
      case DecimalType() => "DECIMAL(18, 2)"
      case BooleanType => "BIT"
      case StringType => "NVARCHAR(MAX)"
      case BinaryType => "VARBINARY(MAX)"
      case DateType => "DATE"
      case TimestampType => "DATETIME2"
      case TimestampNTZType => "DATETIME2"
      case ArrayType(_, _) => "NVARCHAR(MAX)" // Store arrays as JSON strings
      case MapType(_, _, _) => "NVARCHAR(MAX)" // Store maps as JSON strings
      case StructType(_) => "NVARCHAR(MAX)" // Store structs as JSON strings
      case _ => "NVARCHAR(MAX)" // Default fallback
    }

    if (nullable) s"$baseType NULL" else s"$baseType NOT NULL"
  }

  /**
   * Convert CaseInsensitiveStringMap to Scala Map[String, String]
   */
  private def convertCaseInsensitiveMapToScala(ciMap: CaseInsensitiveStringMap): Map[String, String] = {
    val scalaMap = scala.collection.mutable.Map[String, String]()
    ciMap.forEach((key, value) => scalaMap(key) = value)
    scalaMap.toMap
  }

  /**
   * Convert InternalRow to Row for bulk copy compatibility
   */
  private def internalRowToRow(internalRow: InternalRow): Row = {
    Row.fromSeq(
      (0 until schema.length).map(i => internalRow.get(i, schema(i).dataType))
    )
  }
}

/**
 * SQLServerWriterCommitMessage carries information about a partition's write
 */
case class SQLServerWriterCommitMessage(partitionId: Int, rowCount: Long) extends WriterCommitMessage
