package com.paradise.db.schema

import com.paradise.db.Db
import com.paradise.model.GraphEdge

// Defines the schema for the table in the standard way outlined in the Slick documentation
// http://slick.lightbend.com/doc/3.2.0/schemas.html
trait EdgesTable {
  this: Db =>

  import config.profile.api._

  class Edges(tag: Tag) extends Table[GraphEdge](tag, "edges") {
    def index = column[Int]("idx", O.PrimaryKey)

    def relationType = column[String]("rel_type")

    def nodeOne = column[Int]("node_1")

    def nodeTwo = column[Int]("node_2")

    def * = (index,
      relationType,
      nodeOne,
      nodeTwo) <> (GraphEdge.tupled, GraphEdge.unapply)

  }

  val edges = TableQuery[Edges]

}
