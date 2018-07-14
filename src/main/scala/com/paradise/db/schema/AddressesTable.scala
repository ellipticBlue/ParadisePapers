package com.paradise.db.schema

import com.paradise.db.Db
import com.paradise.model.DBNode

// Defines the schema for the table in the standard way outlined in the Slick documentation
// http://slick.lightbend.com/doc/3.2.0/schemas.html
trait AddressesTable {
  this: Db =>

  import config.profile.api._

  val addresses = TableQuery[Addresses]

  class Addresses(tag: Tag) extends Table[DBNode](tag, "nodes.address") {
    def * = (labels,
      validUntil,
      countryCodes,
      countries,
      nodeId,
      sourceId,
      address,
      name,
      jurisdictionDescription,
      serviceProvider,
      jurisdiction,
      closedDate,
      incorporationDate,
      ibcRUC,
      nodeType,
      status,
      companyType,
      note) <> (DBNode.tupled, DBNode.unapply)

    def nodeId = column[Int]("n.node_id", O.PrimaryKey)

    def labels = column[String]("labels(n)")

    def validUntil = column[String]("n.valid_until")

    def countryCodes = column[String]("n.country_codes")

    def countries = column[String]("n.countries")

    def sourceId = column[String]("n.sourceID")

    def address = column[String]("n.address")

    def name = column[String]("n.name")

    def jurisdictionDescription = column[String]("n.jurisdiction_description")

    def serviceProvider = column[String]("n.service_provider")

    def jurisdiction = column[String]("n.jurisdiction")

    def closedDate = column[String]("n.closed_date")

    def incorporationDate = column[String]("n.incorporation_date")

    def ibcRUC = column[String]("n.ibcRUC")

    def nodeType = column[String]("n.type")

    def status = column[String]("n.status")

    def companyType = column[String]("n.company_type")

    def note = column[String]("n.note")
  }

}
