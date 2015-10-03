import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.event.LoggingReceive
import scala.util.control.Breaks
import java.security.MessageDigest
import com.typesafe.config.ConfigFactory
import scala.collection.mutable.ArrayBuffer
import akka.actor.Terminated
import java.util.Random
import akka.util.Timeout
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future;
import scala.util.Success
import scala.util.Failure
object  Watcher {
  case object Completed;
}

class Watcher(time:Long) extends Actor{
  def receive = {
    case Watcher.Completed =>
      println(System.currentTimeMillis - time);
  }
}

object Project2 extends App {
  val system = ActorSystem.create("NodesSystem", ConfigFactory.load(ConfigFactory.parseString("""{ "akka" : { "actor" : { "provider" : "akka.remote.RemoteActorRefProvider" }, "remote" : { "enabled-transports" : [ "akka.remote.netty.tcp" ], "netty" : { "tcp" : { "port" : 12300 } } } } } """)))
  var n = 10;
  var topology = "3D";
  var failurePercentage = 0;
  if(args.size>=1){
    n = args(0).toInt
  }

  var startNode: ActorRef = null;


  if(args.size>=2){
    topology = args(1)
  }
  // create topology
  topology match {
    case "full" =>
      var nodes = new Array[ActorRef](n)
      var nodeNames = new Array[String](n);
      for( i <- 0 to nodes.size -1){
        nodeNames(i) = "Node"+i;
        nodes(i) = system.actorOf(Props(new Node(nodeNames(i),i)), name = nodeNames(i))
      }
      for( i <- 0 to nodes.size -1){
        nodes(i) ! Node.ConnectedNodes(nodeNames)
      }
      startNode = nodes(0)
    case "3D" =>
      startNode = build3D(n,false)
      
    case "line" =>
      startNode = buildLine(n)
    case "imp3D" =>
      startNode = build3D(n,true)

  }


  var algorithm = "gossip";
  

  if(args.size>=3)
    algorithm = args(2);


  if(args.size>=4){
    failurePercentage = args(3).toInt
    val failedNodes = n*failurePercentage/100;
    for(i <- 0 until failedNodes){
      val r = new Random().nextInt(n)

      implicit val timeout = FiniteDuration(1,"seconds")
      system.actorSelection("user/Node"+r).resolveOne(timeout).onComplete {
        case Success(actor) => 
          actor ! Node.Stop
        case Failure(ex) =>
      }
    }
  }


  val watcher = system.actorOf(Props(new Watcher(System.currentTimeMillis)), name = "Watcher");
  algorithm match {
    case "gossip" =>
      var i =0 
      startNode ! GossipNode.Start("Fire!")
      // startNode ! GossipNode.Start("Water!")

    case "push-sum" =>
      startNode ! Node.Start

    case _ => println("Invalide algorithm") 
  }

  def build3D(n:Int,extra:Boolean):ActorRef = {
      var width = math.ceil(math.cbrt(n)).toInt
      val nodes = Array.ofDim[ActorRef](width,width,width)
      var nodeNames = Array.ofDim[String](width,width,width)
      var l = 0
      for {
        i <- 0 until width;
        j <- 0 until width;
        k <- 0 until width
      } {
        if(l<n){

          nodeNames(i)(j)(k) = "Node"+l;
          nodes(i)(j)(k) = system.actorOf(Props(new Node(nodeNames(i)(j)(k),l)), name = nodeNames(i)(j)(k))
          if(i>0){
            nodes(i)(j)(k) ! Node.Connect(nodeNames(i-1)(j)(k))
            nodes(i-1)(j)(k) ! Node.Connect(nodeNames(i)(j)(k))
          }
          if(j>0){
            nodes(i)(j-1)(k) ! Node.Connect(nodeNames(i)(j)(k))
            nodes(i)(j)(k) ! Node.Connect(nodeNames(i)(j-1)(k))
          }
          if(k>0){
            nodes(i)(j)(k-1) ! Node.Connect(nodeNames(i)(j)(k))
            nodes(i)(j)(k) ! Node.Connect(nodeNames(i)(j)(k-1))
          }
          if(extra)
            nodes(i)(j)(k) ! Node.Connect("Node"+new Random().nextInt(n))

        }else{
          return nodes(0)(0)(0)
        }
        l+=1;
      }




      return nodes(0)(0)(0)
  }

   def buildLine(n:Int):ActorRef = {
      var width = n
      val nodes = Array.ofDim[ActorRef](width)
      var nodeNames = Array.ofDim[String](width)
      for {
        i <- 0 until width
      } {
          nodeNames(i) = "Node"+i;
          nodes(i) = system.actorOf(Props(new Node(nodeNames(i),i)), name = nodeNames(i))
          if(i>0){
            nodes(i) ! Node.Connect(nodeNames(i-1))
            nodes(i-1) ! Node.Connect(nodeNames(i))
          }
        }
      return nodes(0)
  }
}