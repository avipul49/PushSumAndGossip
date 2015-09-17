import org.scalatest.{ BeforeAndAfterAll, FlatSpecLike, Matchers }
import akka.actor.{ Actor, Props, ActorSystem }
import akka.testkit.{ ImplicitSender, TestKit, TestActorRef }
import scala.concurrent.duration._

class BitcoinMiningSpec(_system: ActorSystem)
  extends TestKit(_system)
  with ImplicitSender
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("BitcoinMiningSpec"))

  override def afterAll: Unit = {
    system.shutdown()
    system.awaitTermination(10.seconds)
  }

  
}
