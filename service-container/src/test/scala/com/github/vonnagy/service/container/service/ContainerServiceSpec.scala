package com.github.vonnagy.service.container.service

import akka.actor.{ActorSystem, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.stream.ActorMaterializer
import com.github.vonnagy.service.container.{AkkaTestkitSpecs2Support, TestUtils}
import com.typesafe.config.ConfigFactory
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import org.specs2.mutable.SpecificationLike

class ContainerServiceSpec extends AkkaTestkitSpecs2Support(ActorSystem("test", {
  val http = TestUtils.temporaryServerHostnameAndPort()

  ConfigFactory.parseString(
  s"""
      container.http.interface="${http._2}"
      container.http.port=${http._3}
    """)})) with SpecificationLike with FutureMatchers {

  sequential
  val cont = new ContainerService(Nil, Nil, name = "test")

  "The ContainerService" should {

    "create the appropriate parts during construction" in {
      cont.registeredHealthChecks must be equalTo Nil
      cont.registeredRoutes must be equalTo Nil
      cont.started must beFalse
    }

    "start properly and respond to a `/ping` request" in {
      cont.start()
      cont.started must beTrue

      val host = system.settings.config.getString("container.http.interface")
      val port = system.settings.config.getInt("container.http.port")

      implicit val materializer = ActorMaterializer()

      val resp = Http().singleRequest(HttpRequest(uri = s"http://$host:$port/ping"))
      resp.value.get.get.status must eventually(be_==(StatusCodes.OK))

    }

    "shut down properly when asked" in {
      cont.shutdown
      implicit val ec = ExecutionEnv.fromExecutionContext(system.dispatcher)
      cont.system.whenTerminated must beAnInstanceOf[Terminated].await
    }
  }
}
