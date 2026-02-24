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
import org.apache.spark.sql.connector.write.LogicalWriteInfo
import org.apache.spark.sql.connector.write.WriteBuilder
import org.apache.spark.sql.connector.write.Write
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap
import scala.util.Try

/**
 * SQLServerWriteBuilder implements the WriteBuilder interface for SQL Server bulk write operations.
 * It builds a BatchWrite instance that handles the actual write execution.
 * Automatically detects SaveMode (Overwrite, Append, Ignore, ErrorIfExists) for truncation handling.
 */
class SQLServerWriteBuilder(val options: CaseInsensitiveStringMap,
                            val info: LogicalWriteInfo) extends WriteBuilder with Logging {

  private var sortOrder: Array[String] = Array()
  private var partitioningColumns: Array[String] = Array()
  
  // Detect SaveMode using reflection (available in LogicalWriteInfo internals)
  private val saveMode = detectSaveMode()
  private val dbtable = options.get("dbtable")
  private val schema = info.schema()
  
  logDebug(s"SQLServerWriteBuilder created with SaveMode=$saveMode, dbtable=$dbtable")
  
  /**
   * Detect SaveMode from LogicalWriteInfo using multiple strategies
   * 1. Try to access the mode field through reflection (internal API)
   * 2. Check common configuration keys used by Spark
   * 3. Default to ErrorIfExists if not found
   */
  private def detectSaveMode(): SaveMode = {
    try {
      // Strategy 1: Try to access the mode field through reflection (internal API)
      val modeFieldTry = Try {
        val field = info.getClass.getDeclaredField("mode")
        field.setAccessible(true)
        field.get(info).asInstanceOf[SaveMode]
      }
      
      modeFieldTry match {
        case scala.util.Success(mode) =>
          logDebug(s"SaveMode detected via reflection: $mode")
          return mode
        case scala.util.Failure(_) =>
          logDebug("Could not detect SaveMode via reflection, trying alternative methods")
      }
      
      // Strategy 2: Check if SaveMode is encoded in the options (some Spark versions do this)
      val mode = Try {
        info.getClass.getDeclaredMethod("mode").invoke(info).asInstanceOf[SaveMode]
      }
      
      mode match {
        case scala.util.Success(m) =>
          logDebug(s"SaveMode detected via getter method: $m")
          m
        case scala.util.Failure(_) =>
          logDebug("SaveMode not found in LogicalWriteInfo, using default (ErrorIfExists)")
          SaveMode.ErrorIfExists
      }
    } catch {
      case e: Exception =>
        logWarning(s"Error detecting SaveMode: ${e.getMessage}, using default")
        SaveMode.ErrorIfExists
    }
  }

  /**
   * Build method creates and returns a BatchWrite instance as a Write
   */
  override def build(): Write = {
    logDebug("SQLServerWriteBuilder.build() called")
    new SQLServerBatchWrite(options, schema, saveMode)
  }
}
