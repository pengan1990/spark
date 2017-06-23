# Auth Client for Thrift server　　
because there have to get the logic plan to justify the authorization, so have to modify the source code in spark session.
  
And here to record what are changed in spark source code.  

## catalyst project : spark-catalyst_2.11  


### AstBuilder 语法解析

#### class AstBuilder 中增加成员变量

* 增加两个成员变量
```$xslt
val schemaTables = mutable.HashMap[String, mutable.ArrayBuffer[String]]()
val noSchemaTables = mutable.ArrayBuffer[String]()
```

* visitTableIdentifier 函数中增加如下代码
```$xslt
    // 获取当前操作的库名和表明
    if (Option(ctx.db).nonEmpty) {
      // if db is not empty
      if (schemaTables.get(ctx.db.getText).isEmpty) {
        schemaTables.put(ctx.db.getText, mutable.ArrayBuffer[String]())
      }
      schemaTables(ctx.db.getText) += ctx.table.getText
    } else {
      // db is empty just put into
      noSchemaTables += ctx.table.getText
    }
```

* visitSingleStatement 作为语法解析入口
```$xslt
    // 清空上一次语法解析留下来的数据 避免污染此次解析数据
    schemaTables.clear()
    noSchemaTables.clear()
```

* ParserDriver.scala 文件的 class AbstractSqlParser中 增加函数
```$xslt
  // 供外部调用tableSchema
  def tableSchema :mutable.HashMap[String, mutable.ArrayBuffer[String]] = astBuilder.schemaTables
  def noTableSchema :mutable.ArrayBuffer[String] = astBuilder.noSchemaTables
```

## core project: spark-sql_2.11 

### Dataset 数据集  

#### object Dataset 

* ofRows(sparkSession: SparkSession, logicalPlan: LogicalPlan) 函数中增加验证方法 auth(sparkSession) 

```$xslt
  /**
    * user db tables access check
    *
    * @param sparkSession
    */
  def auth(sparkSession: SparkSession): Unit = {
    /**
      * 获取当前库信息
      */
    val currDb = sparkSession.sessionState.catalog.getCurrentDatabase
    /**
      * 非DEFAULT 库 才有权限控制{因为thriftCliServer默认采用default 库}
      */
    val schemaTables = sparkSession.sessionState.sqlParser.asInstanceOf[AbstractSqlParser].tableSchema
    val noSchemaTables = sparkSession.sessionState.sqlParser.asInstanceOf[AbstractSqlParser].noTableSchema
    val schemas = new java.util.LinkedHashMap[String, java.util.List[String]]

    schemaTables.foreach( f => {
      val tables:java.util.List[String] = f._2.asJava
      schemas.put(f._1, tables)
    })

    if (noSchemaTables.nonEmpty) {
      // 当前逻辑库不为空　并且 不带库名表内容也不为空 不用考虑sql 之间的逻辑关系 {没有use db 直接show tables}
      val noTbs:java.util.List[String] = noSchemaTables.asJava
      if (schemas.get(currDb) == null) {
        schemas.put(currDb, noTbs)
      } else {
        schemas.get(currDb).addAll(noTbs)
      }
    }
    // password is ""
    AuthHttpClient.verify(sparkSession.conf.get(sparkSession.sqlContext.SPARK_SQL_HIVE_USER),
      sparkSession.conf.get(sparkSession.sqlContext.SPARK_SQL_HIVE_PASSWORD), schemas)
  }

```

## Take User and Password from Session

### SQLContext.scala {class SQLContext}
* Two const string Set to SQLContext

```$xslt
  // spark sql hive user
  val SPARK_SQL_HIVE_USER: String = "spark.sql.hive.user"
  val SPARK_SQL_HIVE_PASSWORD : String = "spark.sql.hive.password"
```

* which the upstairs code is called by 

```$xslt
  def setConf(key: String, value: String): Unit = {
    sparkSession.conf.set(key, value)
  }
```

### SparkSessionManager.scala {class **SparkSQLSessionManager** }

* set User name and Password {**OpenSession** method}
```$xslt
81     if (username == null) {
          throw new AuthException(s"user name [" + username
            + "] or passwd [" + passwd + "] is null")
        }
        var password = passwd
        if (passwd == null) {
          password = ""
        }
    
        ctx.setConf(ctx.SPARK_SQL_HIVE_USER, username)
91      ctx.setConf(ctx.SPARK_SQL_HIVE_PASSWORD, password)
```

### Init {AuthHttpClient.java}

to Init auth http client properties like the followings
```$xslt
spark.thrift.server.auth.url=http://127.0.0.1:5741/auth/verify/
spark.thrift.server.auth.show.databases.url=http://127.0.0.1:5741/auth/show/databases/
spark.thrift.server.auth.show.tables.url=http://127.0.0.1:5741/auth/show/tables/
spark.thrift.server.auth.show.columns.url=http://127.0.0.1:5741/auth/show/columns/
```

Which is called in **HiveThriftServe2.scala**  **main** method

```$xslt
95  AuthHttpClient.init()
```


