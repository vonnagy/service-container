package com.github.vonnagy.service.container.http.routing

import akka.actor.ActorDSL._
import akka.actor._
import akka.testkit.{TestActorRef, TestProbe}
import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import org.specs2.mutable.SpecificationLike

/**
  * Created by Ivan von Nagy on 1/15/15.
  */
class RoutedEndpointsActorSpec extends AkkaTestkitSpecs2Support with SpecificationLike {

  "The RoutedEndpointsActor" should {

    "allow actor to add routes" in {
      val probe = TestProbe()

      val svc = TestActorRef(new Act {
        become {
          case _ =>
        }
      }, "service")

      val routeSvc = svc.underlyingActor.context
        .actorOf(Props(new Act with RoutedService {
          become(routeReceive)
        }), "http")

      val act = TestActorRef(new RoutedEndpointsActor {
        def receive = {
          case RouteAdded => probe.ref ! RouteAdded
        }

        override def route = {
          path("test") {
            complete("complete")
          }
        }
      })

      probe.expectMsg(RouteAdded) must beEqualTo(RouteAdded)

    }
  }
}

class TestRoutedEndpoints(implicit val system: ActorSystem,
                          actorRefFactory: ActorRefFactory) extends RoutedEndpoints {

  override def route = {
    path("route") {
      complete("route")
    }
  }
}
