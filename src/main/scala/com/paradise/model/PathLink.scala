package com.paradise.model

// Represents an individual link in a path chain with the final link in the chain not having a connecting edge.
case class PathLink (vertex: GraphNode,
                     connectingEdge: Option[Edge])
