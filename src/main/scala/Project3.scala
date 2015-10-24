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


object  Watcher {
  case class Completed(node:String,hops:Double);
}

class Watcher(n:Int) extends Actor{
  var r = 0;
  var sum = 0.0;
  def receive = {
    case Watcher.Completed(node,hops) =>
      println(r+"-> Avg hops for node: "+node+" is "+hops);
      r = r+1;
      sum = sum+hops;
      if(r==n){
        println("Finished with total avg "+(sum/n));
      }
  }
}

object Project3 extends App {
  val system = ActorSystem.create("NodesSystem", ConfigFactory.load(ConfigFactory.parseString("""{ "akka" : { "actor" : { "provider" : "akka.remote.RemoteActorRefProvider" }, "remote" : { "enabled-transports" : [ "akka.remote.netty.tcp" ], "netty" : { "tcp" : { "port" : 12301 } } } } } """)))
 
  val n = args(0).toInt
  val m = args(1).toInt
  var entryPoint = system.actorOf(Props(new Node("0",m)), name = "0")
  entryPoint ! Node.Init
  system.actorOf(Props(new Watcher(n)), name = "Watcher")

  var max = 20;
  var nodes = new Array[String](n) 
  for(i <- 1 until n+1){

    var nodeName = ""+(new Random().nextInt(1 << max - 1))
    while(nodes contains nodeName){
      nodeName = ""+(new Random().nextInt(1 << max - 1))
    }
    nodes(i-1) = nodeName
    val nnode = system.actorOf(Props(new Node(nodeName,m)), name = nodeName)
    nnode ! Node.Start
    // entryPoint ! Node.Join(nodeName)
  }
  entryPoint ! Node.Start

  for(i <- 0 until n){
    entryPoint ! Node.Join(nodes(i))
  }

  // for(i <- 0 until n+1){
  //   system.actorSelection("user/"+i) ! Node.Print
  // }
}