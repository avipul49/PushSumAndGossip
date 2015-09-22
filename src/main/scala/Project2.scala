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

object Project2 extends App {
  val system = ActorSystem.create("NodesSystem", ConfigFactory.load(ConfigFactory.parseString("""{ "akka" : { "actor" : { "provider" : "akka.remote.RemoteActorRefProvider" }, "remote" : { "enabled-transports" : [ "akka.remote.netty.tcp" ], "netty" : { "tcp" : { "port" : 12300 } } } } } """)))
  var n = 10;
  if(args.size>=1){
    n = args(0).toInt
  }

  var nodes = new Array[ActorRef](n)
  var nodeNames = new Array[String](n);

  var algorithm = "gossip";
  if(args.size>=2)
    algorithm = args(1);
  algorithm match {
    case "gossip" =>
      var i =0 
      for( i <- 0 to nodes.size -1){
        nodeNames(i) = "Node"+i;
        nodes(i) = system.actorOf(Props(new GossipNode(nodeNames(i))), name = nodeNames(i))
      }

      for( i <- 0 to nodes.size -1){
        nodes(i) ! GossipNode.ConnectedNodes(nodeNames)
      }
      nodes(0) ! Node.Weight(1.0)
      nodes(0) ! GossipNode.Start("Fire!")
      nodes(2) ! GossipNode.Start("Water!")

    case "push-sum" =>
      var i =0 
      for( i <- 0 to nodes.size -1){
        nodeNames(i) = "Node"+i;
        nodes(i) = system.actorOf(Props(new Node(nodeNames(i),i)), name = nodeNames(i))
      }

      for( i <- 0 to nodes.size -1){
        nodes(i) ! Node.ConnectedNodes(nodeNames)
      }
      nodes(0) ! Node.Weight(1.0)
      nodes(0) ! Node.Start

    case _ => println("Invalide algorithm") 
  }
}