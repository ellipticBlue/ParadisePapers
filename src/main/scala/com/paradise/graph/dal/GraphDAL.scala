package com.paradise.graph.dal

import com.paradise.graph.{Graph, GraphConfiguration}
import com.paradise.model._
import gremlin.scala._

import scala.concurrent.{ExecutionContext, Future}

// DAL for graph store using the Gremlin-Scala project (see https://github.com/mpollmeier/gremlin-scala)
case class GraphDAL(graphConfiguration: GraphConfiguration)(implicit ec: ExecutionContext) extends Graph {

  // Graph property definitions (see documentation at https://github.com/mpollmeier/gremlin-scala)
  // ---
  // Edges
  private val EdgeId = Key[String]("edgeId")

  // Nodes
  // ---
  private val Labels = Key[String]("labels")
  private val ValidUntil = Key[String]("validUntil")
  private val CountryCodes = Key[String]("countryCodes")
  private val Countries = Key[String]("countries")
  private val NodeId = Key[String]("nodeId")
  private val SourceId = Key[String]("sourceId")
  private val Address = Key[String]("address")
  private val Name = Key[String]("name")
  private val JurisdictionDescription = Key[String]("jurisdictionDescription")
  private val ServiceProvider = Key[String]("serviceProvider")
  private val Jurisdiction = Key[String]("jurisdiction")
  private val ClosedDate = Key[String]("closedDate")
  private val IncorporationDate = Key[String]("incorporationDate")
  private val IbcRUC = Key[String]("ibcRUC")
  private val NodeType = Key[String]("nodeType")
  private val Status = Key[String]("status")
  private val CompanyType = Key[String]("companyType")
  private val Note = Key[String]("note")

  // Graph Schema Management
  // ---
  // Removes all edges and vertices from underlying graph
  def clearGraph(): Unit = {
    graph.E.toList().foreach(_.remove())
    graph.tx.commit()
    graph.tx.readWrite()

    graph.V.toList().foreach(_.remove())
    graph.tx.commit()
    graph.tx.readWrite()
  }

  // Graph edge extractors - Following methods extract edge data from the graph
  // store for various different parameter sets returning GraphEdge for further handling
  // ---
  // Extracts edge given edge ID as it is defined in the original RDBMS for the edge
  def getDBEdgeFromId(edgeId: Int): Option[GraphEdge] = {
    // Query graph for edge that has matching EdgeId property
    graph.E.has(EdgeId, P.eq[String](edgeId.toString)).headOption() match {
      case Some(e) =>
        Some(getDBEdge(e))
      case _ =>
        None
    }
  }

  // Extracts GraphEdge given internal Gremlin representation of that edge
  def getDBEdge(edge: Edge): GraphEdge = {
    getDBEdge(
      graph.E(edge.id()).outV().head(),
      edge,
      graph.E(edge.id()).inV().head())
  }

  // Helper method for other edge extractor methods that uses Gremlin
  // representations of surrounding vertices along with the edge itself
  // to extract a GraphEdge
  private def getDBEdge(tailVert: Vertex,
                edge: Edge,
                headVert: Vertex): GraphEdge =
    GraphEdge(
      edge.property(EdgeId).value().toInt,
      edge.label(),
      tailVert.property(NodeId).value().toInt,
      headVert.property(NodeId).value().toInt
    )


  // Graph edge extractors - Following methods extract edge data from the graph
  // store for various different parameter sets returning GraphEdge for further handling
  // ---
  // Extracts node given node ID as it is defined in the original RDBMS for the node
  def getGraphNodeFromId(nodeId: Int): Option[GraphNode] = {
    getVertexFromId(nodeId) match {
      case Some(v) =>
        Some(getGraphNode(v))
      case _ =>
        None
    }
  }

  // Extracts Gremlin representation of node node ID as it is defined in the original RDBMS for the node
  def getVertexFromId(nodeId: Int): Option[Vertex] =
    graph.V.has(NodeId, P.eq[String](nodeId.toString)).headOption()

  // Extracts GraphNode given Gremlin representation
  def getGraphNode(v: Vertex): GraphNode = {
    val vm: Map[String, String] = v.valueMap.map(i => i._1 -> i._2.toString)

    GraphNode(
      v.label(),
      vm.getOrElse("labels", ""),
      vm.getOrElse("validUntil", ""),
      vm.getOrElse("countryCodes", ""),
      vm.getOrElse("countries", ""),
      vm.getOrElse("nodeId", "0").toInt,
      vm.getOrElse("sourceId", ""),
      vm.getOrElse("address", ""),
      vm.getOrElse("name", ""),
      vm.getOrElse("jurisdictionDescription", ""),
      vm.getOrElse("serviceProvider", ""),
      vm.getOrElse("jurisdiction", ""),
      vm.getOrElse("closedDate", ""),
      vm.getOrElse("incorporationDate", ""),
      vm.getOrElse("ibcRUC", ""),
      vm.getOrElse("nodeType", ""),
      vm.getOrElse("status", ""),
      vm.getOrElse("companyType", ""),
      vm.getOrElse("note", ""))
  }

  // Graph Write Operations
  // ---
  // Writes a GraphNode into the graph store and returns the nodeId along
  // with the Gremlin representation of the node upon complection
  def addNode(newNode: GraphNode): (Int, Vertex) = {
    val newVert = graph + (
      newNode.nodeLabel,
      Labels -> newNode.labels,
      ValidUntil -> newNode.validUntil,
      CountryCodes -> newNode.countryCode,
      Countries -> newNode.countries,
      NodeId -> newNode.nodeId.toString,
      SourceId -> newNode.sourceId,
      Address -> newNode.address,
      Name -> newNode.name,
      JurisdictionDescription -> newNode.jurisdictionDescription,
      ServiceProvider -> newNode.serviceProvider,
      Jurisdiction -> newNode.jurisdiction,
      ClosedDate -> newNode.closedDate,
      IncorporationDate -> newNode.incorporationDate,
      IbcRUC -> newNode.ibcRUC,
      NodeType -> newNode.nodeType,
      Status -> newNode.status,
      CompanyType -> newNode.companyType,
      Note -> newNode.note)

    graph.tx.commit()
    graph.tx.readWrite()

    (newNode.nodeId, newVert)
  }

  // Writes a GraphEdge into the Graph store and returns an option on the original
  // parameter indicating success or failure of the write
  def addEdge(newEdge: GraphEdge, vertexMap: Map[Int, Vertex]): Option[GraphEdge] = {
    val tailVert = vertexMap.get(newEdge.nodeOne)
    val headVert = vertexMap.get(newEdge.nodeTwo)

    (headVert, tailVert) match {
      case (Some(hv), Some(tv)) =>
        tv --- (newEdge.relationType, EdgeId -> newEdge.index.toString) --> hv

        graph.tx.commit()
        graph.tx.readWrite()

        Some(newEdge)
      case _ =>
        println(s"Failed to add edge ID: ${newEdge.index}")

        None
    }
  }

  // Graph Read Operations
  // ---
  // Extracts all attached edges (any direction) with corresponding adjacent nodes given a Gremlin node representation
  // Parameters:
  //   * v - vertex for which to get attached edges and adjacent nodes in graph
  //   * incomingEdge - optionally-specified edge to exclude (along with associated node) from results. This
  //                    stops back-tracking when using this method to build traversals
  def getAttachedEdgesAndVertices(v: Vertex, incomingEdge: Option[Edge]): Future[List[(Edge, Vertex)]] = {
    val futOutgoing =
      Future(graph.V(v.id()).outE().as("a").inV().as("b").select.toList)
    val futIngoing =
      Future(graph.V(v.id()).inE().as("a").outV().as("b").select.toList)

    val concatFut = for {
      inc <- futIngoing
      out <- futOutgoing
    } yield inc ++ out

    incomingEdge match {
      case Some(e) =>
        concatFut.map(_.filter(_._1.id() != e.id()))
      case _ =>
        concatFut
    }
  }

  // Graph Processing Operations
  // ---
  // Find the shortest path(s) joinging two nodes, given the node IDs of the endpoints.
  // Note - The node IDs are expected to correspond to those in the original RDBMS datastore
  def getShortestPaths(startId: Int, endId: Int): Future[PathCollection] = {

    // TODO: Impelement this

    Future.successful(PathCollection(Set()))
  }
}

