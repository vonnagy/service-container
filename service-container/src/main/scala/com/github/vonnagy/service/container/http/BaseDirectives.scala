package com.github.vonnagy.service.container.http

import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.server._

trait BaseDirectives extends Directives {

  /**
    * This directive can be used to only allow requests that have at least one of the accepted
    * media types. This looks at the requests `Accept` header and matches the values to the
    * allowed ones.
    *
    * @param mediaTypes
    * @return
    */
  def acceptableMediaTypes(mediaTypes: MediaType*): Directive0 =
    extract(_.request.getHeader(classOf[Accept])).flatMap {
      case o if o.isPresent =>
        val mtn = ContentNegotiator(Seq(o.get)).mtn
        val disallowed = mediaTypes.flatMap { t =>
          mtn.isAccepted(t) match {
            case true => None
            case false => Some(t)
          }
        }

        disallowed.size > 0 match {
          case true =>
            val alt = disallowed.map(ContentNegotiator.Alternative(_))
            reject(UnacceptedResponseContentTypeRejection(alt.toSet))
          case false =>
            pass
        }

      case _ => pass
    }

}
