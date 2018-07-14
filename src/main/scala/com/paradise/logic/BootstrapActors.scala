package com.paradise.logic

import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.config.ConfigFactory

object BootstrapActors {

  private val config = ConfigFactory.load()

  case class SystemAndActors(actorSystem: ActorSystem,
                             graphManager: ActorRef)

  def startActors(): SystemAndActors = {
    implicit val actorSystem: ActorSystem = ActorSystem(config.getString("akka.actorSystemName"))

    val graphManagerActor = getGraphManagerActor(actorSystem)

    SystemAndActors(
      actorSystem,
      graphManagerActor)
  }

  private def getGraphManagerActor(as: ActorSystem): ActorRef =
    as.actorOf(
      GraphManager.props,
      GraphManager.Name)

}
