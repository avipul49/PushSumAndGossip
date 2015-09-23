// import akka.actor.Actor
// import akka.actor.ActorRef
// import akka.actor.ActorSystem
// import akka.actor.Props
// import akka.event.LoggingReceive
// import scala.util.control.Breaks
// import java.security.MessageDigest
// import com.typesafe.config.ConfigFactory
// import scala.collection.mutable.ArrayBuffer
// import akka.actor.Terminated
// import java.util.Random

// // object GossipNode{
// //   case class ConnectedNodes(nodes : Array[String])
// //   case class Roumor(message:String)
// //   case class Start(message:String)
// // }

// // class GossipNode(nodeName:String) extends Actor{
// //   var name = nodeName
// //   var connectedNodes = new Array[String](1)
// //   var messageMap: Map[String,Int] = Map()


// //   def receive = {
// //     case GossipNode.ConnectedNodes(nodes) =>
// //       connectedNodes = nodes;
// //     case GossipNode.Roumor(message) => 
// //       val n = messageMap.getOrElse(message, 0)
// //       messageMap+=(message -> (n+1))
// //       println(name + ": " + message+ " " +(n+1))

// //       if(n < 10){
// //         sendRoumor(message)
// //       }
      
// //     case GossipNode.Start(message) =>
// //       sendRoumor(message)
// //   }

// //   def sendRoumor(message : String) = {
// //     var indexToSend = new Random().nextInt(connectedNodes.size)
// //     context.actorSelection("../"+connectedNodes(indexToSend))  ! GossipNode.Roumor(message);
// //   }
// // }
