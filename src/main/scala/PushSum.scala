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

object Node{
  case class ConnectedNodes(nodes : Array[ActorRef])
  case class Sum(sum : Double, weight: Double)
  case object Start
  case class Weight(value :Double)
}
class Node(value : Double) extends Actor{
  var sum = value;
  var weight = 0.0;
  var connectedNodes = new Array[ActorRef](1)
  var messageCount = 0;
  var lastValues = new Array[Int](2)
  def receive = {
    case Node.ConnectedNodes(nodes) =>
      connectedNodes = nodes;
    case Node.Sum(s,w) => 
      var last = sum/weight;
      sum += s;
      weight += w;
      var n = sum/weight;
      if(Math.abs(n-last) < 0.0000000001)
        messageCount+=1;
      else
        messageCount=0;

      println(value+"- "+(sum/weight))
     
      if(messageCount < 3){
        var indexToSend = new Random().nextInt(connectedNodes.size)
        connectedNodes(indexToSend) ! Node.Sum(sum/2,weight/2);
        sum = sum/2
        weight = weight/2
      }
    case Node.Start =>
      var indexToSend = new Random().nextInt(connectedNodes.size)
      if(connectedNodes(indexToSend)!=null){
        connectedNodes(indexToSend) ! Node.Sum(sum/2,weight/2);
        sum = sum/2
        weight = weight/2
      }
    case Node.Weight(v : Double) =>
      weight = v
  }
}

object BitcoinMining extends App {
  var nodes = new Array[ActorRef](5)
  val system = ActorSystem.create("NodesSystem", ConfigFactory.load(ConfigFactory.parseString("""{ "akka" : { "actor" : { "provider" : "akka.remote.RemoteActorRefProvider" }, "remote" : { "enabled-transports" : [ "akka.remote.netty.tcp" ], "netty" : { "tcp" : { "port" : 12300 } } } } } """)))
  var i =0 
  for( i <- 0 to nodes.size -1){
    nodes(i) = system.actorOf(Props(new Node(i)), name = "Node"+i)
  }

  for( i <- 0 to nodes.size -1){
    nodes(i) ! Node.ConnectedNodes(nodes)
  }
  nodes(0) ! Node.Weight(1.0)
  nodes(0) ! Node.Start

}