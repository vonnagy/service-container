package com.github.vonnagy.service.container.http

import net.liftweb.json.ext.JodaTimeSerializers
import spray.http._
import spray.httpx.LiftJsonSupport
import spray.httpx.marshalling.{BasicMarshallers, Marshaller, MetaMarshallers}

trait DefaultMarshallers extends MetaMarshallers with BasicMarshallers {

  val lift = new LiftJsonSupport {
    implicit def liftJsonFormats = net.liftweb.json.DefaultFormats ++ JodaTimeSerializers.all
  }

  implicit def jsonFormats = lift.liftJsonFormats

  def jsonUnmarshaller[T: Manifest] = lift.liftJsonUnmarshaller[T]

  def jsonMarshaller[T <: AnyRef] = lift.liftJsonMarshaller

  def plainMarshaller[T <: Any] =
    Marshaller.delegate[T, String](ContentTypes.`text/plain`)(_.toString)
}
