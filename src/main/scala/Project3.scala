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

object Project3 extends App {
  val system = ActorSystem.create("NodesSystem", ConfigFactory.load(ConfigFactory.parseString("""{ "akka" : { "actor" : { "provider" : "akka.remote.RemoteActorRefProvider" }, "remote" : { "enabled-transports" : [ "akka.remote.netty.tcp" ], "netty" : { "tcp" : { "port" : 12301 } } } } } """)))
  var entryPoint = system.actorOf(Props(new Node("0")), name = "0")
  entryPoint ! Node.Init
  val n = args(0).toInt
  val m = args(1)

  for(i <- 1 until n+1){
    val nodeName = ""+i
    val nnode = system.actorOf(Props(new Node(nodeName)), name = nodeName)
  }

  entryPoint ! Node.Start

  for(i <- 1 until n+1){
    val nodeName = ""+i
    entryPoint ! Node.Join(i+"")
  }


  for(i <- 0 until n+1){
    system.actorSelection("user/"+i) ! Node.Print
  }
}