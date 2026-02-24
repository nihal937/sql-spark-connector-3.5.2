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

import com.microsoft.sqlserver.jdbc.spark.BulkCopyUtils._
import com.microsoft.sqlserver.jdbc.spark.utils.JdbcUtils.createConnection
import org.apache.spark.internal.Logging
import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.connector.write.BatchWrite
import org.apache.spark.sql.connector.write.DataWriterFactory
import org.apache.spark.sql.connector.write.PhysicalWriteInfo
import org.apache.spark.sql.connector.write.Write
import org.apache.spark.sql.connector.write.WriterCommitMessage
import org.apache.spark.sql.execution.datasources.jdbc.JdbcUtils
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap
import java.sql.Connection

/**
 * SQLServerBatchWrite implements the BatchWrite interface for batch write operations.
 * It coordinates the write process across partitions.
 * Automatically handles truncation based on SaveMode (Overwrite mode auto-truncates).
 */
class SQLServerBatchWrite(val options: CaseInsensitiveStringMap,
                          val schema: StructType,
                          val saveMode: SaveMode = SaveMode.ErrorIfExists) extends Write with BatchWrite with Logging {

  logDebug(s"SQLServerBatchWrite initialized with saveMode=$saveMode")

  /**
   * createBatchWriterFactory creates a factory for DataWriter instances
   */
  override def createBatchWriterFactory(info: PhysicalWriteInfo): DataWriterFactory = {
    logDebug(s"createBatchWriterFactory called with saveMode=$saveMode")
    new SQLServerDataWriterFactory(options, schema, saveMode)
  }

  /**
   * commit is called after all partitions are written successfully
   */
  override def commit(messages: Array[WriterCommitMessage]): Unit = {
    logDebug(s"commit called with ${messages.length} commit messages")
    // In DataSourceV2, commit is typically for cleanup or final coordination
    // Most of the heavy lifting happens in DataWriter itself
  }

  /**
   * abort is called if the write fails to allow cleanup
   */
  override def abort(messages: Array[WriterCommitMessage]): Unit = {
    logError(s"abort called with ${messages.length} commit messages after failed write")
    // Cleanup any temporary resources if needed
  }
}
