package com.paradise.db.dal

import com.paradise.db.Db
import com.paradise.db.schema._
import com.paradise.model._
import slick.basic.DatabaseConfig
import slick.dbio.DBIOAction
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

// Provides an abstraction layer over the RDBMS handling components. Method descriptions are given for those
// where the name may not give the entire context
class DbDAL(val config: DatabaseConfig[JdbcProfile])
  extends Db
    with EdgesTable
    with AddressesTable
    with EntitiesTable
    with IntermediariesTable
    with OfficersTable
    with OthersTable {

  import config.profile.api._
  import scala.concurrent.ExecutionContext.Implicits.global

  // Edge queries
  // ---
  def getAllEdges: Future[Seq[GraphEdge]] = db.run(edges.result)

  // Node queries
  // ---
  def getAllAddresses: Future[Seq[GraphNode]] = db.run(addresses.result).map(i => i.map(GraphNode("address", _)))

  def getAllEntities: Future[Seq[GraphNode]] = db.run(entities.result).map(i => i.map(GraphNode("entity", _)))

  def getAllIntermediaries: Future[Seq[GraphNode]] = db.run(intermediaries.result).map(i => i.map(GraphNode("intermediary", _)))

  def getAllOfficers: Future[Seq[GraphNode]] = db.run(officers.result).map(i => i.map(GraphNode("officer", _)))

  def getAllOthers: Future[Seq[GraphNode]] = db.run(others.result).map(i => i.map(GraphNode("other", _)))

  def closeDb(): Unit = db.close()

  // For testing - Project only requires read operations to be performed on DB, but some additional methods are needed
  // to make unit tests. The methods below exist solely for this purpose

  // Creates all the DB tables
  def initAll: Future[Unit] =
    db.run(DBIOAction.seq(
      edges.schema.create,
      addresses.schema.create,
      entities.schema.create,
      intermediaries.schema.create,
      officers.schema.create,
      others.schema.create
    ))

  // Deletes all the DB tables
  def dropAll: Future[Unit] =
    db.run(DBIOAction.seq(
      edges.schema.drop,
      addresses.schema.drop,
      entities.schema.drop,
      intermediaries.schema.drop,
      officers.schema.drop,
      others.schema.drop
    ))

  // Inserts an edge into the edge table
  def insertEdge(e: GraphEdge): Future[Int] =
    db.run(edges += e)

  // As all the node tables have the same schema, tests can be made a bit more concise with an operation to write
  // a given node to all the tables
  def insertNodeIntoAll(n: DBNode): Future[Unit] = {
    db.run(DBIOAction.seq(
      addresses += n,
      entities += n,
      intermediaries += n,
      officers += n,
      others += n
    ))
  }
}

