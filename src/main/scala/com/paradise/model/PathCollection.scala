package com.paradise.model

// Represents a collection of paths through the graph structure
// Note: Path collections handle shortest path degeneracy by simply reporting back all discovered shortests paths
case class PathCollection (paths: Set[Path])
