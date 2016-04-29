package com.github.vonnagy.service.container.http

import java.util.UUID

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaTypes
import com.github.vonnagy.service.container.http.json.Json4sSupport
import org.json4s.JsonAST.{JNull, JString}
import org.json4s.ext.JodaTimeSerializers
import org.json4s.{CustomSerializer, DefaultFormats, JValue, jackson}

trait DefaultMarshallers {

  import Json4sSupport._

  implicit val serialization = jackson.Serialization

  // The implicit formats used for serialization. This can be overridden
  implicit def defaultJsonFormats = DefaultFormats ++ JodaTimeSerializers.all ++ List(UUIDSerializer)

  def jsonUnmarshaller[T: Manifest] = json4sUnmarshaller

  def jsonMarshaller[T <: AnyRef]: ToEntityMarshaller[T] = json4sMarshaller

  implicit def jsonValueMarshaller[T <: JValue]: ToEntityMarshaller[T] = json4sJValueMarshaller

  def jsonStringMarshaller[String]: ToEntityMarshaller[String] =
    Marshaller.StringMarshaller.wrap(MediaTypes.`application/json`)(_.toString)

  def plainMarshaller[T <: Any]: ToEntityMarshaller[T] =
    Marshaller.StringMarshaller.wrap(MediaTypes.`text/plain`)(_.toString)

  case object UUIDSerializer extends CustomSerializer[UUID](format => ( {
    case JString(u) => UUID.fromString(u)
    case JNull => null
    case u => UUID.fromString(u.toString)
  }, {
    case u: UUID => JString(u.toString)
  }
    ))

}
