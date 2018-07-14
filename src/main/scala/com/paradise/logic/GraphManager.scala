package com.paradise.logic

import akka.actor.{Actor, ActorRef, Props}
import com.paradise.db.DbConfiguration
import com.paradise.db.dal.DbDAL
import com.paradise.graph.GraphConfiguration
import com.paradise.graph.dal.GraphDAL
import com.paradise.logic.GraphManager.GraphManagerState
import com.paradise.logic.Protocols.GraphManagerProtocol._
import com.paradise.model.{GraphEdge, GraphNode, PathCollection, PathRequest}

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

// Actor that manages requests on the graph store.
// Notes -
//    * Uses futures to implement a lighter-weight actor cameo pattern. Concurrency in the actor is avoided by
//      the requirement that the thread running the actor can not use any future results directly in its internal
//      processing. Any data needed to be returned by a future to the actor thread is sent via an actor message.
//    * Internal actor state is managed using 'become' to update the receive handler with state updates.
//    * Graph ingest operations can take a long time to return. So, acknowledgement is passed back to the
//      end-user on request receipt instead of at the conclusion of the operation. Moreover, the actor state is set to
//      'busy' so that clear/re-ingest requests can't be piled up on the system. Actor is also set into a 'busy' state
//      when clearing the graph.

object GraphManager {

  val Name = "graph-manager"

  def props(): Props =
    Props(new GraphManager())

  case class GraphManagerState(graphBusy: Boolean) {
    def setBusy(newBusyState: Boolean) =
      GraphManagerState(newBusyState)
  }

}

class GraphManager() extends Actor with DbConfiguration {

  val dBdal = new DbDAL(config)
  val graphDal = GraphDAL(GraphConfiguration("paradisegraph"))

  // Clear the underlying graph store
  private def clearGraph(origin: ActorRef): Future[Unit] = {
    val resPromise = Promise[Unit]()

    // Create a future around the clear graph call and complete the result promise with it
    val resultFut = Future {
      graphDal.clearGraph()
    }

    resPromise completeWith resultFut

    // Once the clearing future completes send acknowledgement back to end-user and send message
    // to GraphManager actor that the graph is no long busy
    resultFut onComplete {
      case Success(_) =>
        self ! GraphWorkComplete
        origin ! Acknowledgement("Graph successfully cleared")
      case Failure(t) =>
        self ! GraphWorkComplete
        origin ! Acknowledgement(s"Graph clearing failed with ${t.getMessage}")
    }

    resPromise.future
  }

  // Re-ingest data from the RDBMS into the graph store
  private def reingestData(origin: ActorRef): Future[Unit] = {
    val resultPromise = Promise[Unit]()

    // Get futures for extracting all edges and nodes from the RDBMS and use these to complete an intimediary
    // promise that will subsequently kickoff the ingest of the data into the Graph store
    val paradiseNodes = Future.sequence(Seq(
      dBdal.getAllAddresses,
      dBdal.getAllEntities,
      dBdal.getAllIntermediaries,
      dBdal.getAllOfficers,
      dBdal.getAllOthers)).map(_.flatten)

    val paradiseEdges = dBdal.getAllEdges

    val intermediaryPromise = Promise[(Seq[GraphNode], Seq[GraphEdge])]()

    intermediaryPromise completeWith {
      for {
        nds <- paradiseNodes
        eds <- paradiseEdges
      } yield (nds, eds)
    }

    // When the intermediary promise completes (i.e. all edges and nodes extracted from the RDBMS),
    // start the graph ingest using the current thread. This is just an easy way to ensure the edges
    // are added only after all the nodes have been. We could speed this up, but since this is a once-off
    // operation (in theory), it's acceptable for the moment. Once ingest is complete send message to the
    // GraphManager actor that the graph is no longer busy
    intermediaryPromise.future onComplete {
      case Success((nodes, edges)) =>
        resultPromise completeWith Future {
          origin ! Acknowledgement(s"Ingesting ${nodes.size} nodes and ${edges.size} edges into graph. This will take several minutes to complete.")
          val nodeMap = nodes.map(graphDal.addNode).toMap
          edges.foreach(graphDal.addEdge(_, nodeMap))
          self ! GraphWorkComplete
        }
      case _ =>
        resultPromise completeWith Future {
          origin ! Acknowledgement(s"Failed to extract all node and edge date from DB, halting ingest.")
          self ! GraphWorkComplete
        }
    }

    resultPromise.future
  }

  // Fulfill end-user request to find shortest distance between two graph nodes. This is just a wrapper method
  private def calculateShortestPath(origin: ActorRef, pathRequest: PathRequest): Unit = {
    graphDal.getShortestPaths(pathRequest.startNodeId, pathRequest.endNodeId) onComplete {
      case Success(res) =>
        origin ! res
      case _ =>
        origin ! PathCollection(Set())
    }
  }

  override def receive: Receive = graphManagerHandler(GraphManagerState(false))

  protected def graphManagerHandler(state: GraphManagerState): Receive = {

    case msg: GraphManagerMessage =>
      msg match {
        case ClearGraph =>
          // Received API request to clear graph
          val origin = sender()

          if (state.graphBusy) {
            origin ! Acknowledgement("Graph store is currently busy. Please wait and try again.")
          } else {
            context.become(graphManagerHandler(state.setBusy(true)))
            clearGraph(origin)
          }

        case ReingestData =>
          // Received API request to re-ingest data from RDBMS into Graph store
          val origin = sender()
          if (state.graphBusy) {
            origin ! Acknowledgement("Graph store is currently busy. Please wait and try again.")
          } else {
            context.become(graphManagerHandler(state.setBusy(true)))
            reingestData(origin)
          }

        case FindShortestPath(pr) =>
          // Received API request to find shortest path between two graph nodes
          val origin = sender()
          calculateShortestPath(origin, pr)

        case GraphWorkComplete =>
          // Future returning information saying graph processing is complete
          context.become(graphManagerHandler(state.setBusy(false)))

        case _ =>
      }
    case _ =>
  }
}

