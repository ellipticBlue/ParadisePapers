package com.paradise.graph
import com.typesafe.config.{Config, ConfigFactory}

// Defines a Graph configuration taken from appication.conf
// Parameters:
//   * path - root path to use in extracting parameters from application.conf
case class GraphConfiguration(path: String) {
  val config: Config = ConfigFactory.load()
}

