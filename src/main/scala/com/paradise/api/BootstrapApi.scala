package com.paradise.api

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

// Simple object to bootstrap the systems around the REST API
object BootstrapApi {
  def startApi(actorSystem: ActorSystem,
               graphManager: ActorRef) = {

    implicit val as: ActorSystem = actorSystem
    implicit val mat: ActorMaterializer = ActorMaterializer()

    val config = ConfigFactory.load()

    import actorSystem.dispatcher

    implicit val http: HttpExt = Http(actorSystem)
    implicit val routingSettings: RoutingSettings = RoutingSettings(actorSystem)

    val route = HttpEndpoint.routes(graphManager, Duration(config.getInt("api.timeout"), SECONDS))

    http.bindAndHandle(route, config.getString("api.host"), config.getInt("api.port"))
  }
}
