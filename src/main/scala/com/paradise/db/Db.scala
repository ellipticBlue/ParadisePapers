package com.paradise.db

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

// Trait that defines a DB instance. Follows a pattern described here:
// https://medium.com/@kennajenifer1234/scala-tutorial-create-crud-with-slick-and-mysql-1b0a5092899f
trait Db {
  val config: DatabaseConfig[JdbcProfile]
  val db: JdbcProfile#Backend#Database = config.db
}
