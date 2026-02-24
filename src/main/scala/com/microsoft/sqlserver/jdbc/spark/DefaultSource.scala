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
import org.apache.spark.sql.connector.catalog.Table
import org.apache.spark.sql.connector.expressions.Transform
import org.apache.spark.sql.sources.DataSourceRegister
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap

/**
 * DefaultSource implements DataSourceV2 to provide a high-performance SQL Server and Azure SQL connector.
 * This supports both read and write operations, with optimized bulk write capabilities.
 * Read functionality leverages Spark's standard JDBC connector.
 * Read for datapool external tables is supported from Master instance that's handled via JDBC logic.
 */
class DefaultSource extends DataSourceRegister with Logging {

    /**
     * shortName provides an alias to access the MSSQL Spark connector 
     */
    override def shortName(): String = "mssql"

    /**
     * getTable creates a Table object representing the SQL Server table for DataSourceV2 operations
     */
    def getTable(options: CaseInsensitiveStringMap,
                 partitioning: Array[Transform],
                 schema: StructType): Table = {
      logDebug(s"getTable called with dbtable=${options.get("dbtable")}")
      new SQLServerTable(options, schema)
    }
}
