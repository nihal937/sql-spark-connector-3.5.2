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

import org.apache.spark.internal.Logging
import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.connector.write.DataWriter
import org.apache.spark.sql.connector.write.DataWriterFactory
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap

/**
 * SQLServerDataWriterFactory creates DataWriter instances for each partition.
 * Passes SaveMode to writers for automatic truncation handling on overwrite operations.
 */
class SQLServerDataWriterFactory(val options: CaseInsensitiveStringMap,
                                 val schema: StructType,
                                 val saveMode: SaveMode = SaveMode.ErrorIfExists) extends DataWriterFactory with Logging {

  /**
   * createWriter creates a new DataWriter for a specific partition
   */
  override def createWriter(partitionId: Int, taskId: Long): DataWriter[InternalRow] = {
    logDebug(s"Creating writer for partition $partitionId, task $taskId with saveMode=$saveMode")
    new SQLServerDataWriter(partitionId, taskId, options, schema, saveMode)
  }
}
