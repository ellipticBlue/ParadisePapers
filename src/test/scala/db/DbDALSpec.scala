package db

import com.paradise.db.dal.DbDAL
import com.paradise.model.GraphNode
import org.scalatest._

import DataFixture._

import scala.concurrent.Await
import scala.concurrent.duration._

class DbDALSpec extends FlatSpec with Matchers with BeforeAndAfter with DBTestConfiguration {

  val testDal = new DbDAL(config)

  before {
    Await.result(testDal.initAll, Duration.Inf)
    Await.result(testDal.insertEdge(edge1), Duration.Inf)
    Await.result(testDal.insertEdge(edge2), Duration.Inf)
    Await.result(testDal.insertNodeIntoAll(node1), Duration.Inf)
    Await.result(testDal.insertNodeIntoAll(node2), Duration.Inf)
  }

  after {
    Await.result(testDal.dropAll, Duration.Inf)
  }

  "DB DAL" should "extract edges correctly" in {
    Await.result(testDal.getAllEdges, Duration.Inf).toSet shouldBe Set(edge1, edge2)
  }

  "DB DAL" should "extract addresses correctly" in {
    Await.result(testDal.getAllAddresses, Duration.Inf).toSet shouldBe Set(GraphNode("address", node1), GraphNode("address", node2))
  }

  "DB DAL" should "extract entities correctly" in {
    Await.result(testDal.getAllEntities, Duration.Inf).toSet shouldBe Set(GraphNode("entity", node1), GraphNode("entity", node2))
  }

  "DB DAL" should "extract intermediaries correctly" in {
    Await.result(testDal.getAllIntermediaries, Duration.Inf).toSet shouldBe Set(GraphNode("intermediary", node1), GraphNode("intermediary", node2))
  }

  "DB DAL" should "extract officers correctly" in {
    Await.result(testDal.getAllOfficers, Duration.Inf).toSet shouldBe Set(GraphNode("officer", node1), GraphNode("officer", node2))
  }

  "DB DAL" should "extract others correctly" in {
    Await.result(testDal.getAllOthers, Duration.Inf).toSet shouldBe Set(GraphNode("other", node1), GraphNode("other", node2))
  }
}
