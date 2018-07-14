package com.paradise.logic

import akka.actor.{Actor, ActorRef, Props}
import com.paradise.logic.GraphManager.GraphManagerState
import com.paradise.logic.Protocols.GraphManagerProtocol._
import com.paradise.model.{PathCollection, PathRequest}

import scala.concurrent.{Future, Promise}

// Actor that manages requests on the graph store.
// Notes -
//    * Uses futures to implement a lighter-weight actor cameo pattern. Concurrency in the actor is avoided by
//      the requirement that the thread running the actor can not use any future results directly in its internal
//      processing. Any data needed to be returned by a future to the actor thread is sent via an actor message.
//    * Internal actor state is managed using 'become' to update the receive handler with state updates.
//    * Graph clear and ingest operations can take a long time to return. So, acknowledgement is passed back to the
//      end-user on request receipt instead of at the conclusion of the operation. Moreover, the actor state is set to
//      'busy' so that clear/re-ingest requests can't be piled up on the system.

object GraphManager {

  val Name = "graph-manager"

  def props(): Props =
    Props(new GraphManager())

  case class GraphManagerState(graphBusy: Boolean) {
    def setBusy(newBusyState: Boolean) =
      GraphManagerState(newBusyState)
  }

}

class GraphManager() extends Actor {

  // Clear the underlying graph store
  private def clearGraph(origin: ActorRef): Future[Unit] = {
    val resP = Promise[Unit]()

    // TODO: Implement

    self ! GraphWorkComplete
    origin ! Acknowledgement("clearGraph needs implementation")

    resP.future
  }

  // Re-ingest data from the RDBMS into the graph store
  private def reingestData(origin: ActorRef): Future[Unit] = {
    val resultPromise = Promise[Unit]()

    // TODO: Implement

    self ! GraphWorkComplete
    origin ! Acknowledgement("reingestData needs implementation")

    resultPromise.future
  }

  // Fulfill end-user request to find shortest distance between two graph nodes
  private def getShortestPath(origin: ActorRef, pathRequest: PathRequest) = {

    // TODO: Implement
    origin ! PathCollection(Set())
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
          getShortestPath(origin, pr)

        case GraphWorkComplete =>
          // Future returning information saying graph processing is complete

          context.become(graphManagerHandler(state.setBusy(false)))

        case _ =>
      }
    case _ =>
  }
}

