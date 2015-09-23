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
  case class ConnectedNodes(nodes : Array[String])
  case class Connect(node: String)
  case class Sum(sum : Double, weight: Double)
  case object Start
  case class Weight(value :Double)
}

object GossipNode{
  case class ConnectedNodes(nodes : Array[String])
  case class Roumor(message:String)
  case class Start(message:String)
}

class Node(name:String,value : Double) extends Actor{
  var sum = value;
  var weight = 0.0;
  var connectedNodes: Seq[String] = Seq()
  var messageCount = 0;
  var messageMap: Map[String,Int] = Map()

  def receive = {
    case Node.ConnectedNodes(nodes) =>
      connectedNodes ++= nodes;

    case Node.Connect(node) =>
      connectedNodes :+= node

    case Node.Sum(s,w) => 
      var last = sum/weight;
      sum += s;
      weight += w;
      var n = sum/weight;
      if(Math.abs(n-last) < 0.0000000001)
        messageCount+=1;
      else
        messageCount=0;

      println(name+": "+(sum/weight))
     
      if(messageCount < 4){
        pushSum();
      }
    case Node.Start =>
      weight = 1.0
      pushSum();
     
    case Node.Weight(v : Double) =>
      weight = v

    case GossipNode.Roumor(message) => 
      val n = messageMap.getOrElse(message, 0)
      messageMap+=(message -> (n+1))
      println(name + ": " + message+ " " +(n+1))

      if(n < 10){
        sendRoumor(message)
      }
      
    case GossipNode.Start(message) =>
      sendRoumor(message)
  }

  def pushSum(){
    var indexToSend = new Random().nextInt(connectedNodes.size)
    context.actorSelection("../"+connectedNodes(indexToSend)) ! Node.Sum(sum/2,weight/2);
    sum = sum/2
    weight = weight/2
  }


  def sendRoumor(message : String) = {
    var indexToSend = new Random().nextInt(connectedNodes.size)
    context.actorSelection("../"+connectedNodes(indexToSend))  ! GossipNode.Roumor(message);
  }
}
