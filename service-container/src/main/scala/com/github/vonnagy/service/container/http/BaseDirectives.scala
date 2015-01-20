package com.github.vonnagy.service.container.http

import spray.http.{ContentType, MediaType, MediaTypes}
import spray.routing._

trait BaseDirectives extends Directives {

  val respondJson = respondWithMediaType(MediaTypes.`application/json`)
  val respondPlain = respondWithMediaType(MediaTypes.`text/plain`)

  /**
   * This directive can be used to only allow requests that have at least one of the accepted
   * media types. This looks at the requests `Accept` header and matches the values to the
   * allowed ones.
   *
   * @param mediaTypes
   * @return
   */
  def acceptableMediaTypes(mediaTypes: MediaType*): Directive0 = {
    val rejection = UnacceptedResponseContentTypeRejection(mediaTypes.map(ContentType(_)))
    extract(_.request.acceptableContentType(mediaTypes.map(ContentType(_)))).flatMap {
      case Some(_) => pass
      case _ => reject(rejection)
    }
  }
}
