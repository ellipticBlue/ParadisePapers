package graph

import java.util.concurrent.{ExecutorService, Executors}

import com.paradise.graph.GraphConfiguration
import com.paradise.graph.dal.GraphDAL
import org.scalatest.{FlatSpec, Matchers}
import DataFixture._
import gremlin.scala.Vertex

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

class GraphDALSpec extends FlatSpec with Matchers {

  implicit val ec: ExecutionContext {
    def reportFailure(t: Throwable): Unit

    val threadPool: ExecutorService

    def execute(runnable: Runnable): Unit
  } = new ExecutionContext {
    val threadPool: ExecutorService = Executors.newFixedThreadPool(4)

    def execute(runnable: Runnable) {
      threadPool.submit(runnable)
    }

    def reportFailure(t: Throwable) {}
  }

  val graphDal = GraphDAL(GraphConfiguration("testgraph"))

  graphDal.clearGraph()

  val nodeSet = Set(
    node1,
    node2,
    node3,
    node4,
    node5,
    node6,
    node7,
    node8,
    node9)

  val edgeSet = Set(
    edge1,
    edge2,
    edge3,
    edge4,
    edge5,
    edge6,
    edge7,
    edge8,
    edge9,
    edge10)

  val nodeMap: Map[Int, Vertex] = nodeSet.map(graphDal.addNode).toMap

  "Graph DAL" should "add all nodes to graph" in {
    nodeMap.size shouldBe 9
  }

  "Graph DAL" should "convert vertices to GraphNodes" in {
    nodeMap.map(i => graphDal.getGraphNode(i._2)).toSet shouldBe nodeSet
  }

  "Graph DAL" should "retrieve node from graph given a valid node ID" in {
    graphDal.getGraphNodeFromId(5) shouldBe Some(node5)
  }

  "Graph DAL" should "return None asked to retrieve an invalid node ID" in {
    graphDal.getGraphNodeFromId(11) shouldBe None
  }

  "Graph DAL" should "add all edges to graph" in {
    edgeSet.flatMap(graphDal.addEdge(_, nodeMap)).size shouldBe 10
  }

  "Graph DAL" should "retrieve edge from graph given a valid edge ID" in {
    graphDal.getDBEdgeFromId(3) shouldBe Some(edge3)
  }

  "Graph DAL" should "return None asked to retrieve an invalid edge ID" in {
    graphDal.getDBEdgeFromId(12) shouldBe None
  }

  "Graph DAL" should "get all adjacent edges and vertices" in {
    val adjacent = Await.result(graphDal.getAttachedEdgesAndVertices(nodeMap(4), None), Duration.Inf)

    adjacent.map(i => (graphDal.getDBEdge(i._1), graphDal.getGraphNode(i._2))).toSet shouldBe Set(
      (edge3, node1),
      (edge4, node3),
      (edge7, node7),
      (edge6, node5))
  }

  "Graph DAL" should "find all shortest paths between two connected nodes in graph" in {
    Await.result(graphDal.getShortestPaths(2, 7), Duration.Inf) shouldBe shortestPathCollectionSolution
  }

  "Graph DAL" should "return empty path collection when finding path between two disconnected nodes" in {
    Await.result(graphDal.getShortestPaths(2, 9), Duration.Inf) shouldBe emptyPathCollection
  }

  "Graph DAL" should "return single node path collection" in {
    Await.result(graphDal.getShortestPaths(8, 8), Duration.Inf) shouldBe singleNodePathCollectionSoltuion
  }
}

