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
  case class Sum(sum : Double, weight: Double)
  case object Start
  case class Weight(value :Double)
}
class Node(name:String,value : Double) extends Actor{
  var sum = value;
  var weight = 0.0;
  var connectedNodes = new Array[String](1)
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

      println(name+": "+(sum/weight))
     
      if(messageCount < 3){
        pushSum();
      }
    case Node.Start =>
      pushSum();
     
    case Node.Weight(v : Double) =>
      weight = v
  }

  def pushSum(){
    var indexToSend = new Random().nextInt(connectedNodes.size)
    context.actorSelection("../"+connectedNodes(indexToSend)) ! Node.Sum(sum/2,weight/2);
    sum = sum/2
    weight = weight/2
  }
}
