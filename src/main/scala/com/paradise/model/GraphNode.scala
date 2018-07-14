package com.paradise.model

// Class for internal handling and API output of node data coming from the graph store
object GraphNode {
  def apply(nodeLabel: String, dbNode: DBNode): GraphNode =
    GraphNode(
      nodeLabel,
      dbNode.labels,
      dbNode.validUntil,
      dbNode.countryCode,
      dbNode.countries,
      dbNode.nodeId,
      dbNode.sourceId,
      dbNode.address,
      dbNode.name,
      dbNode.jurisdictionDescription,
      dbNode.serviceProvider,
      dbNode.jurisdiction,
      dbNode.closedDate,
      dbNode.incorporationDate,
      dbNode.ibcRUC,
      dbNode.nodeType,
      dbNode.status,
      dbNode.companyType,
      dbNode.note)
}

case class GraphNode(nodeLabel: String,
                     labels: String,
                     validUntil: String,
                     countryCode: String,
                     countries: String,
                     nodeId: Int,
                     sourceId: String,
                     address: String,
                     name: String,
                     jurisdictionDescription: String,
                     serviceProvider: String,
                     jurisdiction: String,
                     closedDate: String,
                     incorporationDate: String,
                     ibcRUC: String,
                     nodeType: String,
                     status: String,
                     companyType: String,
                     note: String)
