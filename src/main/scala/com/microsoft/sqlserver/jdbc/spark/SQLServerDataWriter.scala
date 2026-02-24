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
import org.apache.spark.sql.types.StructType
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
        // Get column metadata from an empty result set to align with table schema
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
