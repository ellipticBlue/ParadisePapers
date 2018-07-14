package com.paradise.logic

import com.paradise.model.PathRequest

// Defines all classes passed as messages between actors in the application
object Protocols {
  object GraphManagerProtocol {
    sealed trait GraphManagerMessage

    // General message class used to report status back to end-user
    case class Acknowledgement(msg: String) extends GraphManagerMessage

    // Passed to graph manager to initiate search for shortest path
    case class FindShortestPath(req: PathRequest) extends GraphManagerMessage

    // Initiates an asynchronous process to delete all edges and nodes from the underlying graph store
    case object ClearGraph extends GraphManagerMessage

    // Initiates a re-ingest of data from the RDBMS into the graph store
    case object ReingestData extends GraphManagerMessage

    // Passed back by futures to indicate completion of work
    case object GraphWorkComplete extends GraphManagerMessage
  }
}
