import akka.actor.{ActorRefFactory, ActorSystem}
import com.github.vonnagy.service.container.ContainerBuilder
import com.github.vonnagy.service.container.http.routing._
import com.typesafe.config.ConfigFactory
import spray.http.MediaTypes._


object RestHttpSample extends App {

  // Here we establish the container and build it while
  // applying extras.
  val service = new ContainerBuilder()
    // Add a config to override the host and port as well as setup SSL
    .withConfig(ConfigFactory.parseString(
    s"""
      container.http.ssl.enabled=off
      container.http.ssl.key-password="changeme"
      container.http.ssl.key-store="${getClass.getClassLoader.getResource("keystore").getPath}"
      container.http.ssl.key-store-password="changeme"
      container.http.ssl.trust-store="${getClass.getClassLoader.getResource("truststore").getPath}"
      container.http.ssl.trust-store-password="changeme"
      container.http.interface = "localhost"
      container.http.port = "9092"
    """.stripMargin))
    // Add some endpoints
    .withRoutes(classOf[ProductEndpoints]).build

  service.start

  // A product entity
  case class Product(id: Option[Int], name: String)

  class ProductEndpoints(implicit system: ActorSystem,
                         actorRefFactory: ActorRefFactory) extends RoutedEndpoints {

    // Import the default Json marshaller and un-marshaller
    implicit val marshaller = jsonMarshaller
    implicit val unmarshaller = jsonUnmarshaller[Product]

    val route = {
      pathPrefix("products") {
        pathEndOrSingleSlash {
          get {
            // This is a path like ``http://api.somecompany.com/products`` and will fetch all of the products
            respondWithMediaType(`application/json`) {
              complete(Seq(Product(Some(1001), "Widget 1"), Product(Some(1002), "Widget 2")))
            }
          } ~
            post {
              // Simulate the creation of a product. This call is handled in-line and not through the per-request handler.
              entity(as[Product]) { product =>
                respondWithMediaType(`application/json`) {
                  complete(Product(Some(1001), product.name))
                }
              }
            }
        } ~
          path(IntNumber) { productId =>
            get {
              acceptableMediaTypes(`application/json`) {
                // This is the path like ``http://api.somecompany.com/products/1001`` and will fetch the specified product
                respondWithMediaType(`application/json`) {
                  // Push the handling to another context so that we don't block
                  complete(Product(Some(productId), "Widget 1"))
                }
              }
            }
          }

      }
    }
  }

}