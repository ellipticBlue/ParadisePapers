package com.paradise.model

// Class for the internal manipulation of edges and structuring of API output
case class Edge(index: Int,
                relationType: String,
                nodeOne: Int,
                nodeTwo: Int)
