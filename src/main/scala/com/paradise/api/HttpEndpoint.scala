package com.paradise.api

import com.paradise.logic.Protocols.GraphManagerProtocol.{Acknowledgement, ClearGraph, FindShortestPath, ReingestData}
import com.paradise.model.{PathCollection, PathRequest}
import akka.actor.ActorRef
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.settings.RoutingSettings
import akka.pattern._
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

// Defines the routes for the REST API
object HttpEndpoint extends Directives with ApiJsonSupport {
  def routes(graphManager: ActorRef, askTimeout: FiniteDuration)(implicit mat: ActorMaterializer, ec: ExecutionContext, rs: RoutingSettings): Route = {
    Route.seal(
      path("clearGraph") {
        pathEndOrSingleSlash {
          get {
            complete {
              graphManager.ask(ClearGraph)(askTimeout).mapTo[Acknowledgement]
            }
          }
        }
      } ~
        path("ingestData") {
          pathEndOrSingleSlash {
            get {
              complete {
                graphManager.ask(ReingestData)(askTimeout).mapTo[Acknowledgement]
              }
            }
          }
        } ~
        path("getShortestPath") {
          pathEndOrSingleSlash {
            post {
              entity(as[PathRequest]) { data =>
                complete(graphManager.ask(FindShortestPath(data))(askTimeout).mapTo[PathCollection])
              }
            }
          }
        })
  }
}
