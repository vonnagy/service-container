package com.github.vonnagy.service.container.http

import java.lang.reflect.InvocationTargetException

import net.liftweb.json._
import net.liftweb.json.ext.JodaTimeSerializers
import spray.http._
import spray.httpx.marshalling.{BasicMarshallers, Marshaller, MetaMarshallers}
import spray.httpx.unmarshalling._

object DefaultMarshallers extends DefaultMarshallers

trait DefaultMarshallers extends MetaMarshallers with BasicMarshallers {

  implicit def jsonFormats = net.liftweb.json.DefaultFormats ++ JodaTimeSerializers.all

  def plainMarshaller[T <: Any] =
    Marshaller.delegate[T, String](ContentTypes.`text/plain`)(_.toString)

  def jsonUnmarshaller[T: Manifest] = {
    Unmarshaller[T](MediaTypes.`application/json`) {
      case x: HttpEntity.NonEmpty ⇒
        val jsonSource = x.asString(defaultCharset = HttpCharsets.`UTF-8`)
        try parse(jsonSource).extract[T]
        catch {
          case MappingException("unknown error", ite: InvocationTargetException) ⇒ throw ite.getCause
        }
    }
  }

  def jsonMarshaller[T <: AnyRef] =
    Marshaller.delegate[T, String](ContentTypes.`application/json`)(Serialization.write(_))

}
