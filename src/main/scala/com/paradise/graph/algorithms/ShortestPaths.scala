package com.paradise.graph.algorithms

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.function.{BiFunction => JavaBiFunction}

import collection.JavaConverters._
import gremlin.scala._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

// Algorithm key points:
// 1. Search for shortest path involves path searching from both start and end nodes that aims to find a
//    connecting node somewhere in the middle of the path. Naively path searching from the starting node only scales
//    as O(n^k), where n is the path length and k the average number of edges attached to any given node (assuming
//    a breadth first traversal that uses any connecting path length to establish an upper pound on traversal depth).
//    Searching from both the start and end nodes will ideally scale as O(2*(n/2)^k)
// 2. Path searching is done in a concurrent fashion with threads allocated so that paths are explored in a breadth
//    over depth fashion. This affords gains in quickly establishing bounds via calculating minimum distances from start
//    and end nodes that have been fully explored.
// 3. Path searching is carried out using an asynchronous branching recursion. These threads update mutable
//    datastructures in atomic, threadsafe transactions that indicate which nodes have been visited in the head and
//    end path searches. While a purely functional solution is possible the memory overhead required for the necessary
//    copying of datastructures becomes prohibitive for large traversals (which also has a significant impact on
//    computation time)
// 4. There is also a mutable datastrcuture that holds which nodes have been found that join an explored
//    path from a head and end search, along with the total path length associated with those two paths. The entries
//    in this datastructure reflect candidate shortest paths and establish upper bounds on the shortest path length.
//    Exploration threads check this datastructure and terminate if their current length of exploration exceeds the
//    currently established upper bound. This calculation also takes into account the exploration depth from the
//    opposing node to lower this upper bound on search paths.

// Additional note: As this is a hybrid approach between functional and non-functional principles, the patterns
//                  are more unorthodox. However, given the size of the graphs we are attempting to traverse,
//                  speed is the most important consideration with memory management following. Specifically, functional
//                  principles are employed so that concurrency can be managed in a more systematic and lucid manner,
//                  while mutable datastructures are used in a minimalist fashion to remove the necessity for large,
//                  blocking copy operations.

object ShortestPaths {

  // Class that forms the values of the mutable hash map. This holds the connecting edge and adjacent vertex
  // of the vertex forming the associated key in the mutable hashmap. The overall idea is that the key-values
  // in the hash map can be back propogated to form sub-chains of shortest paths
  // Parameters:
  //   * previous - Collection of attached edges and adjacent vertices that lie on all discovered shortest paths going
  //                through the associated vertex (key for which this class is the value of)
  //   * pathLength - Path length traversed to get to this vertex
  //   * transactionId - ID given by thread initiating an update to the visited node data structure. This is used to
  //                     help the thread determine if its request ended up replacing an existing entry in the data
  //                     structure
  case class VisitedNodeContext(previous: Set[(Edge, Vertex)], pathLength: Int, transactionId: UUID)

  // Convenience class used in the path reconstruction from mutable datastructures (mostly to make the
  // associated flat mapping clearer to follow)
  private case class RawPath(path: List[(Vertex, Option[Edge])])

  // Class that holds the collection of nodes discovered by both start and end node traversals. These nodes
  // are where the searches join up, with each associated with a collection of shortests paths going through that
  // node.
  // Note - This class contains mutable structures controlled by a general class lock.
  // Parameters:
  //   * joiningVertices - Vertices discovered by start and end node searches that define a subset of current
  //                       shortest paths
  //   * shortestPathLength - Current path length of any discovered, joinging path. Used to limit the depth of ongoing
  //                          traversals from start/end nodes
  //   * forwardSearchMinimumExploredDepth - Minimum depth (i.e. distance from) fully explored from the start node in
  //                                         traversals. This is used to partially establish an upper bound on the
  //                                         path lengths of the traversals from the end node
  //   * backwardSearchMinimumExploredDepth - Minimum depth (i.e. distance from) fully explored from the end node in
  //                                          traversals. This is used to partially establish an upper bound on the
  //                                          path lengths of the traversals from the start node
  private class JoiningNodes(var joiningVertices: Set[Vertex],
                             var shortestPathLength: Int,
                             var forwardSearchMinimumExploredDepth: Int,
                             var backwardSearchMinimumExploreDepthPath: Int) {

    // Updates joining vertices if the associated path length is shorter than any that has been discovered so far. If
    // not the vertex is still inserted to indicate a termination point for both start and end traversals
    def updateJoingingVertices(v: Vertex, newPathLength: Int): Unit = {
      this.synchronized {
        if (newPathLength < shortestPathLength) {
          this.shortestPathLength = newPathLength
          this.joiningVertices = Set(v)
        } else if (newPathLength == shortestPathLength) {
          this.joiningVertices += v
        }
      }
    }

    // Update the minimum, fully-searched exploration depth for the start node traversals
    def updateMinForwardPathLength(newMinimumExplorationDepth: Int): Unit =
      this.synchronized {
        this.forwardSearchMinimumExploredDepth = newMinimumExplorationDepth
      }

    // Update the minimum, fully-searched exploration depth for the end node traversals
    def updateMinBackwardPathLength(newPathLength: Int): Unit =
      this.synchronized {
        this.backwardSearchMinimumExploreDepthPath = newPathLength
      }
  }

  // Function used to atomically check and update entries in the visited node ConcurrentHashMaps
  // for both start and end node traversals.
  // Note - We use Java ConcurrentHashMaps as they are very well adapted for situations requiring high-concurrency
  //        CRUD operations, which is what this algorithm requires.
  // Overall Idea - A vertex reached by a traversal has the explore path length leading to that vertex associated
  //                with it, as well as the previous edge and vertex explored in the traversal. When put up for
  //                entry into the visited nodes datastructure, if the path length is shorter than whats already
  //                been discovered, the previous edge and vertex is inserted into the DB, as they partially define
  //                the new shortest path length discovered. If the path lengths are the same, the previous edge and
  //                vertex are added to the existing, creating a degeneracy in shortest paths. If the path length is
  //                longer then the previous edge and vertex are discarded, as they can not be apart of any shortest
  //                path candidate.
  // Parameters:
  //   * v - Vertex recently visited that needs its value over-written or updated in the visited node hashmap
  //   * vc - Context for the vertex up for insertion.
  private def insertVisitedVertex(v: Vertex, vc: VisitedNodeContext): JavaBiFunction[Vertex, VisitedNodeContext, VisitedNodeContext] {
    def apply(t: Vertex, u: VisitedNodeContext): VisitedNodeContext
  } =
    new JavaBiFunction[Vertex, VisitedNodeContext, VisitedNodeContext]() {
      override def apply(t: Vertex, u: VisitedNodeContext): VisitedNodeContext = {
        if (u == null) {
          vc
        } else if (u.pathLength == vc.pathLength) {
          VisitedNodeContext(vc.previous ++ u.previous, u.pathLength, u.transactionId)
        } else if (u.pathLength < vc.pathLength) {
          u
        } else {
          // We have to return the branch ID of the calling thread, even though this vertex has already been
          // explored, as vertices explored following this one will no longer have the shortest path lengths.
          // So, there is no choice but to re-trace the steps taken in a previous recursion.
          vc
        }
      }
    }

  // Function called by the DAL that returns all the shortest paths given a start and end vertex.
  // Notes -
  //   * Each call contains its own, internal mutable datastructures to manage the start and end traversals.
  //   * The actual steps in each traversal require accessing the graph store to get adjacent edges and nodes,
  //     this function is supplied by the DAL.
  def getShortestPaths(startVertex: Vertex,
                       endVertex: Vertex,
                       getAdjacent: (Vertex, Option[Edge]) => Future[List[(Edge, Vertex)]])(implicit ec: ExecutionContext): Future[Set[List[(Vertex, Option[Edge])]]] = {

    /////////
    // Internal Data Structures
    /////////

    // Promise holding the future that will ultimately return the result.
    val resultShortestPaths = Promise[Set[List[(Vertex, Option[Edge])]]]()

    // Promise that completes when either the start or end search completes (i.e. full explores all depths below the
    // established upper bound on its path lengths). This is used to signal to the traversals coming from the other
    // node that they are to use a different upper bound in limiting their traversals
    val racePromise = Promise[Unit]()

    // Mutable data structures containing visited nodes for both start and end node traversals. At the end of the
    // algorithm these two data structures are used to reconstruct all the shortest paths
    val forwardSearchVisitedNodes = new ConcurrentHashMap[Vertex, VisitedNodeContext]()
    val backwardSearchVisitedNodes = new ConcurrentHashMap[Vertex, VisitedNodeContext]()

    // Threads register their current branch UUID with these along with their current exploration depth. The minimum
    // values of these establish lower bounds for the fully explored depths on start and end node traversals
    val forwardBranchRegister = new ConcurrentHashMap[UUID, Int]()
    val backwardBranchRegister = new ConcurrentHashMap[UUID, Int]()
    val joiningNodes = new JoiningNodes(Set(), Int.MaxValue, 1, 1)

    /////////
    // Internal Methods
    /////////

    // ---
    // Start/End traversal methods
    // For simplicity, we'd like to recurse through a single method for both start and end node traversals. However,
    // the presence of independent, mutable datastructures for both of these breaks some symmetry that would afford
    // a single simple recursion method. So, the parts surrounding the interactions with the respective start and
    // end traversals are separated out here.
    // ---

    // Start node traversal methods
    // ---
    // Returns central visited nodes data structure. This is used to get around parameters being copied for method calls
    def getForwardSearchVisitedNodes: ConcurrentHashMap[Vertex, VisitedNodeContext] =
      forwardSearchVisitedNodes

    // Reconstructs set of shortest paths from the start node to a given joining node by recursive backpropogation.
    // This is used in the final reconstruction of all the shortest paths
    def forwardPathsReconstruction(v: Vertex, e: Option[Edge]): List[List[(Vertex, Option[Edge])]] = {
      if (v == startVertex) {
        List(List((v, e)))
      } else {
        forwardSearchVisitedNodes
          .get(v)
          .previous
          .map(i => forwardPathsReconstruction(i._2, Some(i._1)))
          .reduce(_ ++ _)
          .map(_ ++ List((v, e)))
      }
    }

    // Gets the current minimum exploration depth from the central data structure. Used to get around method
    // parameter copying
    def forwardMinimumExplorationDepth: Int = {
      joiningNodes.forwardSearchMinimumExploredDepth
    }

    // Used to register an ID for a branch as exploring the currentPathLengthDepth
    def forwardRegisterId(id: UUID, currentPathLength: Int): Unit = {
      forwardBranchRegister.put(id, currentPathLength)
    }

    // Used to deregister an ID when associated vertex has been explored
    def forwardUnRegisterId(id: UUID): Unit = {
      forwardBranchRegister.remove(id)

      val forwardValues = forwardBranchRegister.values().asScala

      if (forwardValues.nonEmpty) {
        joiningNodes.updateMinForwardPathLength(forwardValues.min)
      }
    }

    // End node traversal methods
    // ---
    // Returns central visited nodes data structure. This is used to get around parameters being copied for method calls
    def getBackwardSearchVisitedNodes: ConcurrentHashMap[Vertex, VisitedNodeContext] =
      backwardSearchVisitedNodes

    // Reconstructs set of shortest paths from the start node to a given joining node by recursive backpropogation.
    // This is used in the final reconstruction of all the shortest paths
    def backwardPathsReconstruction(v: Vertex, e: Option[Edge]): List[List[(Vertex, Option[Edge])]] = {
      if (v == endVertex) {
        List(List((v, e)))
      } else {
        backwardSearchVisitedNodes
          .get(v)
          .previous
          .map(i => backwardPathsReconstruction(i._2, Some(i._1)))
          .reduce(_ ++ _)
          .map(List((v, e)) ++ _)
      }
    }

    // Gets the current minimum exploration depth from the central data structure. Used to get around method
    // parameter copying
    def backwardMinimumExplorationDepth: Int = {
      joiningNodes.backwardSearchMinimumExploreDepthPath
    }

    // Used to register an ID for a branch as exploring the currentPathLengthDepth
    def backwardRegisterId(id: UUID, currentPathLength: Int): Unit = {
      backwardBranchRegister.put(id, currentPathLength)
    }

    // Used to register an ID for a branch as exploring the currentPathLengthDepth
    def backwardUnRegisterId(id: UUID): Unit = {
      backwardBranchRegister.remove(id)

      val backwardValues = backwardBranchRegister.values().asScala

      if (backwardValues.nonEmpty) {
        joiningNodes.updateMinBackwardPathLength(backwardValues.min)
      }
    }

    // Recursion method used to define the travels from both start and end nodes
    // Parameters
    //   * v - Vertex current being explored in the recursion
    //   * previousEdgeAndVertex - Edge and vertex most recently explored in the recursion chain
    //   * currentPathLength - Current path length from start or end node as defined by the path explored getting to v
    //   * visitedNodes - HashMap containing information on vertices already explored in the traversal
    //   * oppositeVisitedNodes - HashMap containing information on vertices visited by opposing traversal
    //   * oppositeMinimumExplorationDepth - Minimum depth full explored by opposing traversal
    //   * registerId - Method used to register branch ID and current path length with central data store
    //   * unRegisterId - Method to un-register a branch ID
    //   * id - ID generated for current branch by previous recursion step
    def pathStepAsync(v: Vertex,
                      previousEdgeAndVertex: Option[(Edge, Vertex)],
                      currentPathLength: Int,
                      visitedNodes: => ConcurrentHashMap[Vertex, VisitedNodeContext],
                      oppositeVisitedNodes: => ConcurrentHashMap[Vertex, VisitedNodeContext],
                      oppositeMinimumExplorationDepth: => Int,
                      registerId: (UUID, Int) => Unit,
                      unRegisterId: (UUID) => Unit,
                      id: UUID): Future[Unit] = {

      val result = Promise[Unit]()

      // Logic to continue traversal is kept here.
      // - If both traversals are still active, only continue if the pathlength is shorter or equal to the currently
      //   established shortest path length minus the minimum traversal depth of the opposing traversal
      // - If the opposing traversal is completed continue only until all paths to all joining nodes have been explored.
      //   This ensures that all the shortests paths are reported and not just a subset.
      // - If the final answer has already started reconstruction, then we obviously do not continue
      val continueTraveral =
        if (resultShortestPaths.isCompleted) {
          false
        } else if (racePromise.isCompleted) {
          val pathLengths = joiningNodes.joiningVertices.map(i => visitedNodes.get(i).pathLength)

          if (pathLengths.nonEmpty) {
            pathLengths.max >= currentPathLength
          } else {
            false
          }
        } else {
          joiningNodes.shortestPathLength >= (currentPathLength + oppositeMinimumExplorationDepth - 1)
        }

      if (continueTraveral) {
        // Insert information for the current vertex as being visited in the traversal and see if we get our ID
        // returned (means the Vertex hasn't been visited already and we should continue)
        val insertResult = visitedNodes
          .compute(v,
            insertVisitedVertex(v,
              VisitedNodeContext(Set(previousEdgeAndVertex).flatten, currentPathLength, id)))

        if (insertResult.transactionId != id) {
          // Vertex has already been visited in the traversal, so we can stop exploring this path
          unRegisterId(id)
          result completeWith Future.successful(())
        } else {
          // Vertex is unexplored and so we check to see if the opposing traversal has discovered it
          if (oppositeVisitedNodes.containsKey(v)) {
            // Opposing traversal has discovered the vertex, which means this vertex is a joining node.

            // First get the path length to this node from the opposing traversal. This is added to the current
            // path length to get the total path length of the path going through this vertex.
            val otherPathLength = oppositeVisitedNodes.get(v).pathLength

            // Update the joining node data structure with this information
            joiningNodes.updateJoingingVertices(v, otherPathLength + currentPathLength)

            // As this is a connecting vertex, we cease further exploration
            unRegisterId(id)
            result completeWith Future.successful(())
          } else {
            // This vertex has not been explored previously in either traversal. So, we continue on the recursion.

            // First get all adjacent nodes and connecting edges (both directions) to determine next step in branching
            // recursion.
            val adjList = previousEdgeAndVertex match {
              case Some((re, _)) =>
                getAdjacent(v, Some(re))
              case _ =>
                getAdjacent(v, None)
            }

            // When we get the list of vertices and edges to move to next. We generate all the branch IDs for these
            // futures on a single thread that will also deregister the current ID. This ensures that the minimum
            // exploration depth is not misreported, as ID generation and registration may happen some time later
            // if left to their own threads (i.e. ID register can expected to be empty at times).
            adjList onComplete {
              case Success(el) =>

                val resultsWithId = el.map(i => (i, UUID.randomUUID()))

                resultsWithId.foreach { i =>
                  registerId(i._2, currentPathLength + 1)
                }

                // With new IDs registered, we can safely de-register this ID.
                unRegisterId(id)

                // Continue on with the recursion
                Future.sequence {
                  resultsWithId.map { j =>
                    pathStepAsync(
                      j._1._2,
                      Some((j._1._1, v)),
                      currentPathLength + 1,
                      visitedNodes,
                      oppositeVisitedNodes,
                      oppositeMinimumExplorationDepth,
                      registerId,
                      unRegisterId,
                      j._2)
                  }
                } onComplete { _ =>
                  // Complete this result when all sub-branches have been fully explored
                  result completeWith Future.successful(())
                }
              case _ =>
                // Something went wrong with the future. We just complete the resulting future in this case, so
                // that the computation can continue.
                unRegisterId(id)
                result completeWith Future.successful(())
            }
          }
        }
      } else {
        // Are exploration depth is beyond the maximum established to find a shortest path. I.e. we terminate
        // as there is no chance that we will find a path that is the same length or shorter than what is already
        // discovered.
        result completeWith Future.successful(())
      }

      result.future
    }

    // Reconstructs all the shortests paths through a given vertex. Paths are reconstructed by combining the
    // back-propogations from the joining vertex (v) to start and end vertices.
    def getShortestPathsThroughVertex(v: Vertex): Future[List[RawPath]] = {
      val forwardPaths = Future {
        forwardPathsReconstruction(v, None).map(_.init)
      }

      val backPaths = Future {
        backwardPathsReconstruction(v, None).map { i =>
          val iShifted = i.tail ++ List((v, None))
          i.zip(iShifted).map(j => (j._1._1, j._2._2))
        }
      }

      for {
        fp <- forwardPaths
        bp <- backPaths
      } yield fp.flatMap { i =>
        bp.map(j => RawPath(i ++ j))
      }
    }

    /////////
    // Shortest path calculation
    /////////

    // Only proceed with the calculation if we are dealing with a non-trivial request (i.e. start and end nodes not
    // the same). Otherwise we return a trivial path of just the start node.
    if (startVertex.id() != endVertex.id()) {

      // Create the branch IDs to kick off the start and end node traversals
      val forwardId = UUID.randomUUID()
      val backwardId = UUID.randomUUID()

      // Register both IDs with 0 path length
      forwardRegisterId(forwardId, 0)
      backwardRegisterId(backwardId, 0)

      // Create the future for the start node traversal
      val forwardPaths =
        pathStepAsync(
          startVertex,
          None,
          currentPathLength = 0,
          getForwardSearchVisitedNodes,
          getBackwardSearchVisitedNodes,
          backwardMinimumExplorationDepth,
          forwardRegisterId,
          forwardUnRegisterId,
          forwardId)

      // Create the future for the end node traversal
      val backwardPaths =
        pathStepAsync(
          endVertex,
          None,
          currentPathLength = 0,
          getBackwardSearchVisitedNodes,
          getForwardSearchVisitedNodes,
          forwardMinimumExplorationDepth,
          backwardRegisterId,
          backwardUnRegisterId,
          backwardId)

      // First traversal to finish completes the racePromise which will be used by the continuing traversal to
      // alter its maximum path length bound. Also, if one traversal completes with no joinging nodes discovered,
      // this means that the start and end node are not connected by any path. In this situation we complete to
      // result promise with a null path
      forwardPaths onComplete { _ =>
        racePromise tryCompleteWith Future.successful(())
        if (joiningNodes.joiningVertices.isEmpty) {
          resultShortestPaths tryCompleteWith (Future successful Set[List[(Vertex, Option[Edge])]]())
        }
      }

      backwardPaths onComplete { _ =>
        racePromise tryCompleteWith Future.successful(())
        if (joiningNodes.joiningVertices.isEmpty) {
          resultShortestPaths tryCompleteWith (Future successful Set[List[(Vertex, Option[Edge])]]())
        }
      }

      // Once both traversals complete, we reconstruct all the shortest paths
      Future.sequence(Seq(forwardPaths, backwardPaths)) onComplete { _ =>
        resultShortestPaths tryCompleteWith Future.sequence {
          joiningNodes.joiningVertices.map(getShortestPathsThroughVertex)
        }.map(_.flatten.map(_.path))
      }
    } else {
      // Start and end node are the same, so we return a trivial path with just the start node.
      resultShortestPaths completeWith Future.successful(Set(List((startVertex, None))))
    }

    resultShortestPaths.future
  }
}

