package com.paradise.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.paradise.logic.Protocols.GraphManagerProtocol.Acknowledgement
import com.paradise.model._
import spray.json._

// This is used to facilitate the serialization/de-serialization between JSON and case classes
trait ApiJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val printer: PrettyPrinter.type = PrettyPrinter

  implicit lazy val acknowledgeJsonFormat: RootJsonFormat[Acknowledgement] =
    jsonFormat1(Acknowledgement)

  implicit lazy val pathRequestFormat: RootJsonFormat[PathRequest] =
    jsonFormat2(PathRequest)

  implicit lazy val pathCollectionFormat: RootJsonFormat[PathCollection] =
    jsonFormat1(PathCollection)

  implicit lazy val pathFormat: RootJsonFormat[Path] =
    jsonFormat1(Path)

  implicit lazy val pathLinkFormat: RootJsonFormat[PathLink] =
    jsonFormat2(PathLink)

  implicit lazy val edgeFormat: RootJsonFormat[GraphEdge] =
    jsonFormat4(GraphEdge)

  implicit lazy val nodeFormat: RootJsonFormat[GraphNode] =
    jsonFormat19(GraphNode.apply)
}
