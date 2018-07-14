package db

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

// Trait that defines the DB configuration for testing only. Follows a pattern described here:
// https://medium.com/@kennajenifer1234/scala-tutorial-create-crud-with-slick-and-mysql-1b0a5092899f
trait DBTestConfiguration {
  lazy val config = DatabaseConfig.forConfig[JdbcProfile]("testdb")
}
