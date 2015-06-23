package com.github.vonnagy.service.container.http

import net.liftweb.json.Formats
import net.liftweb.json.ext.JodaTimeSerializers
import spray.http._
import spray.httpx.LiftJsonSupport
import spray.httpx.marshalling.{BasicMarshallers, Marshaller, MetaMarshallers}

trait DefaultMarshallers extends MetaMarshallers with BasicMarshallers {

  def liftJson(implicit fmt: Formats): LiftJsonSupport = new LiftJsonSupport {
    implicit def liftJsonFormats = fmt
  }

  // The implicit formats used for serialization. This can be overridden
  implicit def jsonFormats = net.liftweb.json.DefaultFormats ++ JodaTimeSerializers.all

  def jsonUnmarshaller[T: Manifest] = liftJson.liftJsonUnmarshaller[T]

  def jsonMarshaller[T <: AnyRef] = liftJson.liftJsonMarshaller

  def plainMarshaller[T <: Any] =
    Marshaller.delegate[T, String](ContentTypes.`text/plain`)(_.toString)
}
