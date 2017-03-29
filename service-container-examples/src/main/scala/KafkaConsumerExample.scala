import akka.actor.{Actor, Props}
import akka.kafka.ConsumerSettings
import com.github.vonnagy.service.container.ContainerBuilder
import com.github.vonnagy.service.container.health.{GetHealth, HealthInfo, HealthState, RegisteredHealthCheckActor}
import com.github.vonnagy.service.container.log.ActorLoggingAdapter
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer

import scala.util.{Failure, Success, Try}

/**
  * This is an example of how to use the container for a microservice that is a consumer of Kafka messages.
  * The example does NOT run standalone as it is an example and depends on an environment where this a running
  * Zookeeper and Kafka instances.
  * See https://github.com/sclasen/akka-kafka for more information on the consumer.
  */
object KafkaConsumerExample extends App {

  // Here we establish the container and build it while
  // applying extras.
  val service = new ContainerBuilder()
    .withActors(("kafka-consumer-manager", Props[KafkaConsumerManager]))
    .build

  service.start

  /**
    * This is the Kafka Consumer Manager. It will setup and monitor the consumer(s).
    * The actor participates in monitoring the health of the system and this
    */
  class KafkaConsumerManager extends Actor with RegisteredHealthCheckActor with ActorLoggingAdapter {

    lazy val consumerProps = ConsumerSettings.create[String, String](context.system,
      new StringDeserializer, new StringDeserializer)

    var consumer: Try[KafkaConsumer[String, String]] = _

    def receive = {
      // Determine our health
      case GetHealth => sender ! determineHealth
    }

    override def preStart(): Unit = {
      consumer = Try(consumerProps.createKafkaConsumer())
    }

    override def postStop(): Unit = {
      consumer.foreach(_.close())
    }

    /**
      * Return the health of the consumer(s)
      *
      * @return an instance of HealthInfo
      */
    private def determineHealth: HealthInfo = {
      consumer match {
        case Success(c) => HealthInfo("kafka-consumer-manager", HealthState.OK,
          s"Currently connected using ${consumerProps}")

        case Failure(ex) => HealthInfo("kafka-consumer-manager", HealthState.CRITICAL,
          s"Currently not connected: ${ex.getMessage()}")
      }
    }
  }

  class Printer extends Actor {

    def receive = {
      case msg: Any =>
        println(msg)
    }

  }

}