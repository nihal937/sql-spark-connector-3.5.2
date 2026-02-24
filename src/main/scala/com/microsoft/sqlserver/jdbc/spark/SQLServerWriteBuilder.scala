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
import org.apache.spark.sql.connector.write.LogicalWriteInfo
import org.apache.spark.sql.connector.write.WriteBuilder
import org.apache.spark.sql.connector.write.BatchWrite
import org.apache.spark.sql.connector.write.Write
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap

/**
 * SQLServerWriteBuilder implements the WriteBuilder interface for SQL Server bulk write operations.
 * It builds a BatchWrite instance that handles the actual write execution.
 */
class SQLServerWriteBuilder(val options: CaseInsensitiveStringMap,
                            val info: LogicalWriteInfo) extends WriteBuilder with Logging {

  private var sortOrder: Array[String] = Array()
  private var partitioningColumns: Array[String] = Array()

  /**
   * Build method creates and returns a BatchWrite instance as a Write
   */
  override def build(): Write = {
    logDebug("SQLServerWriteBuilder.build() called")
    new SQLServerBatchWrite(options, info.schema())
  }
}
