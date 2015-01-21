import akka.actor.{Actor, Props}
import com.github.vonnagy.service.container.ContainerBuilder
import com.github.vonnagy.service.container.health.{GetHealth, HealthInfo, HealthState, RegisteredHealthCheckActor}
import com.github.vonnagy.service.container.log.ActorLoggingAdapter
import com.sclasen.akka.kafka.{AkkaConsumer, AkkaConsumerProps, StreamFSM}
import kafka.serializer.DefaultDecoder

/**
 * This is an example of how to use the container for a micro-service that is a consumer of Kafka messages.
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

    /*
      The consumer will have 4 streams and max 64 messages per stream in flight, for a total of 256
      concurrently processed messages.
    */
    lazy val consumerProps = AkkaConsumerProps.forSystem(
      system = context.system,
      zkConnect = "localhost:2181",
      topic = "your-kafka-topic",
      group = "your-consumer-group",
      streams = 4, //one per partition
      keyDecoder = new DefaultDecoder(),
      msgDecoder = new DefaultDecoder(),
      receiver = context.actorOf(Props[Printer])
    )

    var consumer: Option[AkkaConsumer[Array[Byte], Array[Byte]]] = None

    def receive = {
      // Determine our health
      case GetHealth => sender ! determineHealth
    }

    override def preStart(): Unit = {

      // Initialize the consumer
      if (consumer.isEmpty) {
        consumer = Some(new AkkaConsumer(consumerProps))
        log.info(s"Connecting on ${consumerProps.zkConnect} to take messages from ${consumerProps.group}:${consumerProps.topicFilterOrTopic}")
        consumer.get.start()
      }
    }

    override def postStop(): Unit = {

      // Shutdown the consumer
      if (consumer.isDefined) {
        log.info("Shutting down consumers")
        consumer.get.stop()
        consumer = None
      }
    }

    /**
     * Return the health of the consumer(s)
     * @return an instance of HealthInfo
     */
    private def determineHealth: HealthInfo = {
      consumer.isDefined match {
        case true => HealthInfo("kafka-consumer-manager", HealthState.OK, s"Currently connected on ${consumerProps.zkConnect} and taking messages from ${consumerProps.group}:${consumerProps.topicFilterOrTopic}")
        case false => HealthInfo("kafka-consumer-manager", HealthState.CRITICAL, s"Currently not connected")
      }
    }
  }

  class Printer extends Actor {

    def receive = {
      case msg: Any =>
        println(msg)
        sender ! StreamFSM.Processed
    }

  }

}