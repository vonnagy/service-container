import akka.actor.{ActorRefFactory, ActorSystem, Props}
import com.github.vonnagy.service.container.ContainerBuilder
import com.github.vonnagy.service.container.http.DefaultMarshallers
import com.github.vonnagy.service.container.http.routing._
import com.github.vonnagy.service.container.log.ActorLoggingAdapter
import spray.http.{MediaTypes, StatusCodes}

// An example use of a basic REST service
object RestHttpSample extends App {

  // Here we establish the container and build it while
  // applying extras.
  val service = new ContainerBuilder()
    // Add some endpoints
    .withRoutes(classOf[ProductEndpoints]).build

  service.start

  // A product entity
  case class Product(id: Option[Int], name: String)

  // A message sent to the handler
  case class GetProduct(id: Option[Int]) extends RestRequest

  class ProductEndpoints(implicit system: ActorSystem,
                         actorRefFactory: ActorRefFactory) extends RoutedEndpoints with DefaultMarshallers {

    // Import the default Json marshaller and un-marshaller
    implicit val marshaller = jsonMarshaller
    implicit val unmarshaller = jsonUnmarshaller[Product]

    val route = {
      pathPrefix("products") {
        get {
          pathEnd {
            // This is a path like ``http://api.somecompany.com/products`` and will fetch all of the products
            respondWithMediaType(MediaTypes.`application/json`) {
              compressResponseIfRequested() {
                ctx =>
                  // Push the handling to another context so that we don't block
                  perRequest[Seq[Product]](ctx, Props(new ProductHandler), GetProduct(None))
              }
            }
          }
        } ~
          path(IntNumber) { productId =>
            acceptableMediaTypes(MediaTypes.`application/json`) {
              // This is the path like ``http://api.somecompany.com/products/1001`` and will fetch the specified product
              respondWithMediaType(MediaTypes.`application/json`) {
                ctx =>
                  // Push the handling to another context so that we don't block
                  perRequest[Product](ctx, Props(new ProductHandler), GetProduct(Some(productId)))
              }
            }
          }
      }
    }
  }

  class ProductHandler extends PerRequestHandler with ActorLoggingAdapter {
    def receive = {
      case GetProduct(Some(id)) =>
        // Return a specific product
        response(Product(Some(id), "Widget 1"), StatusCodes.OK)
      case GetProduct(None) =>
        // Return all products
        response(Seq(Product(Some(1001), "Widget 1"), Product(Some(1002), "Widget 2")), StatusCodes.OK)

    }
  }


}