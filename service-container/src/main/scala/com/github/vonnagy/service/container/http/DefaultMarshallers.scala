package com.github.vonnagy.service.container.http

import java.util.UUID

import net.liftweb.json.JsonAST.JString
import net.liftweb.json._
import net.liftweb.json.ext.JodaTimeSerializers
import spray.http._
import spray.httpx.LiftJsonSupport
import spray.httpx.marshalling.{BasicMarshallers, Marshaller, MetaMarshallers}

trait DefaultMarshallers extends MetaMarshallers with BasicMarshallers {

  def liftJson(implicit fmt: Formats): LiftJsonSupport = new LiftJsonSupport {
    implicit def liftJsonFormats = fmt
  }

  // The implicit formats used for serialization. This can be overridden
  implicit def jsonFormats = net.liftweb.json.DefaultFormats ++ JodaTimeSerializers.all ++ List(UUIDSerializer)

  def jsonUnmarshaller[T: Manifest] = liftJson.liftJsonUnmarshaller[T]

  def jsonMarshaller[T <: AnyRef] = liftJson.liftJsonMarshaller

  def plainMarshaller[T <: Any] =
    Marshaller.delegate[T, String](ContentTypes.`text/plain`)(_.toString)

  case object UUIDSerializer extends CustomSerializer[UUID](format => ( {
    case JString(u) => UUID.fromString(u)
    case JNull => null
    case u => UUID.fromString(u.toString)
  }, {
    case u: UUID => JString(u.toString)
  }
      ))
}
