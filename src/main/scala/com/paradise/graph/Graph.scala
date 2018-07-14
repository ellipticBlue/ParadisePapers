package com.paradise.graph

import com.steelbridgelabs.oss.neo4j.structure.{Neo4JGraphConfigurationBuilder, Neo4JGraphFactory}
import com.steelbridgelabs.oss.neo4j.structure.providers.DatabaseSequenceElementIdProvider
import gremlin.scala._

// Defines a Graph instance
trait Graph {
  val graphConfiguration: GraphConfiguration

  private val graphHost = graphConfiguration.config.getString(graphConfiguration.path + ".host")
  private val graphUser = graphConfiguration.config.getString(graphConfiguration.path + ".user")
  private val graphPass = graphConfiguration.config.getString(graphConfiguration.path + ".password")
  private val graphName = graphConfiguration.config.getString(graphConfiguration.path + ".graphName")

  val graph: ScalaGraph = Neo4JGraphFactory.open {
    Neo4JGraphConfigurationBuilder
      .connect(graphHost, graphUser, graphPass)
      .withName(graphName)
      .withElementIdProvider(classOf[DatabaseSequenceElementIdProvider])
      .build()
  } asScala ()
}
