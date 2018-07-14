package db

import com.paradise.model.{GraphEdge, DBNode}

// Fixture providing data for DB tests
object DataFixture {
  val node1 = DBNode(
    "ab",
    "bc",
    "cd",
    "ef",
    3,
    "gh",
    "ij",
    "kl",
    "mn",
    "op",
    "qr",
    "st",
    "uv",
    "wx",
    "yz",
    "ab",
    "cd",
    "ef")

  val node2 = DBNode(
    "1ab",
    "bc2",
    "cd4",
    "ef",
    12,
    "g3h",
    "1ij",
    "k2l",
    "mn",
    "op7",
    "q8r",
    "st",
    "u9v",
    "w0x",
    "yz",
    "a7b",
    "2cd",
    "e1f")

  val edge1 =
    GraphEdge(2, "cde", 3, 7)

  val edge2 =
    GraphEdge(4, "abc", 1, 2)
}

