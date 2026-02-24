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
import org.apache.spark.sql.connector.catalog.SupportsWrite
import org.apache.spark.sql.connector.catalog.Table
import org.apache.spark.sql.connector.catalog.TableCapability
import org.apache.spark.sql.connector.write.LogicalWriteInfo
import org.apache.spark.sql.connector.write.WriteBuilder
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap
import java.util

/**
 * SQLServerTable represents a SQL Server table as a Spark DataSourceV2 Table.
 * It implements SupportsWrite to provide write capabilities for bulk insert operations.
 */
class SQLServerTable(val options: CaseInsensitiveStringMap,
                     val _schema: StructType) extends Table with SupportsWrite with Logging {

  /**
   * name provides the table name
   */
  override def name(): String = {
    options.get("dbtable") match {
      case opt if opt != null => opt
      case _ => "table"
    }
  }

  /**
   * schema returns the schema of the table
   */
  override def schema(): StructType = _schema

  /**
   * capabilities defines what operations this table supports
   */
  override def capabilities(): util.Set[TableCapability] = {
    util.EnumSet.of(
      TableCapability.BATCH_WRITE,
      TableCapability.TRUNCATE
    )
  }

  /**
   * newWriteBuilder creates a WriteBuilder for batch write operations
   */
  override def newWriteBuilder(info: LogicalWriteInfo): WriteBuilder = {
    logDebug(s"newWriteBuilder called for table ${name()}")
    new SQLServerWriteBuilder(options, info)
  }
}
