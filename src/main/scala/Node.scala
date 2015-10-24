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
import akka.actor._

object Node{
  case object Print
  case object Init	
  case object Stabilize
  case object FixFingers
  case object Start
  case object StartFindKey
  case class FindKey(key:Int,hops:Int,source:String)
  case class KeyFound(target:String,key:Int,hops:Int)
  case class Join(node: String)
  case class Successor(node:String)
  case class Predecessor(node: String)
  case class FindSuccessor(id:String,index: Int)
  case class SetFinger(index:Int,node:String)
  case class GetPredecessor(node:String)
  case class PredecessorNode(node:String)
  case class Inform(node:String)
}

object Settings{
	val MaxNodes = 20;
}

class Node(id: String) extends Actor{
  var m = 20;
  var k = 5;
  var predecessor : String = null
  var fingerTable = new Array[String](m)
  val heartBeat:FiniteDuration = FiniteDuration(1,"milliseconds")
  var messageScheduler : Cancellable = null
  def receive = {
  	case Node.Init =>
  		fingerTable(0) = id
  		predecessor = id 

  	case Node.Start => 
		  //context.system.scheduler.schedule(FiniteDuration(0,"milliseconds"), heartBeat, self, Node.Stabilize)
  		context.system.scheduler.schedule(FiniteDuration(0,"milliseconds"), 10 * heartBeat, self, Node.FixFingers)
  	  messageScheduler = context.system.scheduler.schedule(FiniteDuration(5,"seconds"), FiniteDuration(1,"milliseconds"), self, Node.StartFindKey)

  	case Node.Join(node) =>
  		findSuccessor(node)
  	case Node.Successor(node) =>
  		println("-------- "+node);
  		fingerTable(0) = node
  		context.actorSelection("../"+node) ! Node.Predecessor(id)
  	case Node.Predecessor(node) =>
  		predecessor = node
  	case Node.Print =>
  		println("----------")
  		println(id);
      var i = 
  		println(fingerTable(0));
  		println(predecessor)
  	case Node.Stabilize =>
      if(fingerTable(0) != null){
        context.actorSelection("../"+fingerTable(0)) ! Node.GetPredecessor(id) 
      }
    case Node.GetPredecessor(node) => 
      context.actorSelection("../"+node) ! Node.PredecessorNode(predecessor)

    case Node.PredecessorNode(predecessorNode) =>
      if(isInRange(Hash.key(id),Hash.key(fingerTable(0)),Hash.key(predecessorNode))){
        fingerTable(0) = predecessorNode;
        context.actorSelection("../"+predecessorNode) ! Node.Inform(id)
      }

    case Node.Inform(informedId) =>
      if(isInRange(Hash.key(predecessor),Hash.key(id),Hash.key(informedId)))
        predecessor = informedId;

  	case Node.FixFingers =>
  		val fingerIndex = new Random().nextInt(20)
  		context.actorSelection("../"+fingerTable(0)) ! Node.FindSuccessor(id,fingerIndex)

  	case Node.FindSuccessor(source,fingerIndex) =>
      if(fingerTable(0)!=null&&id!=null){

    		val key = (Hash.key(source) + 1 << fingerIndex)% (1 << m); 

    		if(isInRange(Hash.key(id),Hash.key(fingerTable(0)),key)){
    			val newNode = context.actorSelection("../"+source);
          newNode ! Node.SetFinger(fingerIndex,fingerTable(0))
    		}else{
    			val successor = closestPrecedingFinger(key)
    			context.actorSelection("../"+successor) ! Node.FindSuccessor(source,fingerIndex)
    		}
      }
  	case Node.SetFinger(index,node) =>
  		fingerTable(index) = node;
  	case Node.StartFindKey => 
  		if(fingerTable(0)!=null){
  			val key = new Random().nextInt(1 << m - 1)
  			self ! Node.FindKey(key,0,id)
  		}
  	case Node.FindKey(key:Int,hops: Int,source:String) =>
      if(fingerTable(0)!=null&&id!=null){
      		if(isInRange(Hash.key(id),Hash.key(fingerTable(0)),key)){
      			val newNode = context.actorSelection("../"+source);
    			newNode ! Node.KeyFound(fingerTable(0),key,hops+1)
    		}else{
    			val successor = closestPrecedingFinger(key)
    			context.actorSelection("../"+successor) ! Node.FindKey(key,hops+1,source)
    		}
      }
  	case Node.KeyFound(target:String,key:Int,hops: Int) =>
  		k = k-1
  		if(k==0){
        println("Source "+id+" key found "+key+" target "+target+" in "+hops)
  			messageScheduler.cancel()
      }
  }

  def findSuccessor(node : String) = {
  	var found = false;
  	var successor = fingerTable(0)
  	if(Hash.key(successor) == Hash.key(id) || isInRange(Hash.key(id),Hash.key(successor),Hash.key(node))){
  		found = true;	
  	}
  	if(found){
  		val newNode = context.actorSelection("../"+node)
  		newNode ! Node.Successor(successor)
  		newNode ! Node.Predecessor(id)
  		fingerTable(0) = node;
    }else{
  		successor = closestPrecedingFinger(Hash.key(node))
  		context.actorSelection("../"+successor) ! Node.Join(node)
    }
  }

  def closestPrecedingFinger(key: Int):String = {
  	var i = fingerTable.size - 1
  	var found = false;
  	while(!found&&i>=0){
  		if(fingerTable(i) != null && isInRange(Hash.key(id),key,Hash.key(fingerTable(i)))){
  			return fingerTable(i)
  		}
  		i = i-1;
  	}
  	return null
  }
  def isInRange(start : Int, end : Int, id : Int):Boolean = {
  	if(start <= end){
  		if(id >= start && id<=end){
  			return true;
  		}
  	}else{
  		if(end > id || id > start){
  			return true;
  		}
  	}
  	return false;
  }
}