package com.paradise

import com.paradise.api.BootstrapApi
import com.paradise.logic.BootstrapActors

// Bootstrapping object to startup any necessary services in any sub-packages.
case object Bootstrap {
  def startSystem() = {
    val actorData = BootstrapActors.startActors()

    BootstrapApi.startApi(
      actorData.actorSystem,
      actorData.graphManager)
  }
}
