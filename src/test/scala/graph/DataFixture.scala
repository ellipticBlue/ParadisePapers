package graph

import com.paradise.model._

// Fixture providing data for Graph tests. Contains data to build out a graph along with pre-calculated solutions
// for the shortest path tests.
object DataFixture {
  def genGraphNode(index: Int, label: String) =
    GraphNode(label, "ab", "bc", "cd", "ef", index, "gh", "ij", "kl", "mn", "op", "qr", "st", "uv", "wx", "yz", "ab", "cd", "ef")

  val node1: GraphNode =
    genGraphNode(1, "address")

  val node2: GraphNode =
    genGraphNode(2, "entity")

  val node3: GraphNode =
    genGraphNode(3, "other")

  val node4: GraphNode =
    genGraphNode(4, "officer")

  val node5: GraphNode =
    genGraphNode(5, "entity")

  val node6: GraphNode =
    genGraphNode(6, "address")

  val node7: GraphNode =
    genGraphNode(7, "officer")

  val node8: GraphNode =
    genGraphNode(8, "address")

  val node9: GraphNode =
    genGraphNode(9, "officer")

  val edge1: GraphEdge =
    GraphEdge(1, "foo", 2, 1)

  val edge2: GraphEdge =
    GraphEdge(2, "bar", 3, 1)

  val edge3: GraphEdge =
    GraphEdge(3, "baz", 4, 1)

  val edge4: GraphEdge =
    GraphEdge(4, "baz", 3, 4)

  val edge5: GraphEdge =
    GraphEdge(5, "foo", 1, 6)

  val edge6: GraphEdge =
    GraphEdge(6, "foo", 4, 5)

  val edge7: GraphEdge =
    GraphEdge(7, "bar", 4, 7)

  val edge8: GraphEdge =
    GraphEdge(8, "baz", 7, 6)

  val edge9: GraphEdge =
    GraphEdge(9, "bar", 5, 7)

  val edge10: GraphEdge =
    GraphEdge(10, "baz", 9, 8)

  private val path1: Path =
    Path(List(
      PathLink(node2, Some(edge1)),
      PathLink(node1, Some(edge3)),
      PathLink(node4, Some(edge7)),
      PathLink(node7, None)))

  private val path2: Path =
    Path(List(
      PathLink(node2, Some(edge1)),
      PathLink(node1, Some(edge5)),
      PathLink(node6, Some(edge8)),
      PathLink(node7, None)))

  val shortestPathCollectionSolution: PathCollection =
    PathCollection(Set(path1, path2))

  val singleNodePathCollectionSoltuion: PathCollection =
    PathCollection(Set(Path(List(PathLink(node8, None)))))

  val emptyPathCollection: PathCollection =
    PathCollection(Set())

}

