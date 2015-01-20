package com.github.vonnagy.service.container.http.routing

import akka.actor.ActorDSL._
import akka.actor._
import akka.testkit.{TestProbe, TestActorRef}
import org.specs2.mutable.Specification
import com.github.vonnagy.service.container.TestEndpoints
import spray.routing._
import spray.testkit.Specs2RouteTest

class RoutedServiceSpec extends Specification with Directives with Specs2RouteTest {

  val svcAct = actor("service")(new Act {
    become {
      case _ =>
    }

  })

  val httpAct = TestActorRef(Props(classOf[RoutedService], Seq(new TestEndpoints)), svcAct, "http")
  val svc = httpAct.underlyingActor.asInstanceOf[RoutedService]


  "The RoutedService" should {

    "allow for routes to be defined by a passed sequence of ``RoutedEndpoints`` instances" in {

      Get("/test") ~> svc.buildRoute(svc.routes) ~> check {
        responseAs[String] must be equalTo "test"
      }
    }

    "allow for routes to be added after the system is already loaded" in {
      // This should create the actor and register the endpoints
      val probe = new TestProbe(system)
      val at = system.actorOf(Props(new RoutedServicesActor(probe.ref)), "actor-test")
      probe.expectMsg(RouteAdded)

      val route = svc.routes

      Get("/actortest") ~> svc.buildRoute(svc.routes) ~> check {
        responseAs[String] must be equalTo "actortest"
      }
    }
  }

  step {
    system.shutdown
    system.awaitTermination
  }


  class RoutedServicesActor(probe: ActorRef)(implicit system: ActorSystem,
                                             actorRefFactory: ActorRefFactory) extends RoutedEndpointsActor {

    def receive = {
      case RouteAdded => println(s"The route was added"); probe ! RouteAdded
    }

    override def route = {
      path("actortest") {
        complete("actortest")
      }
    }

    override def registerRoute: Unit =
      context.system.actorSelection("user/service/http") ! AddRoute(this)

  }

}
