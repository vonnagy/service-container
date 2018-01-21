package com.github.vonnagy.service.container.http.directives

import akka.http.scaladsl.model.headers.{`Remote-Address`, `X-Forwarded-For`, `X-Real-Ip`}
import akka.http.scaladsl.server.Directive0
import com.github.vonnagy.service.container.http.BaseDirectives
import com.github.vonnagy.service.container.http.routing.Rejection.NotFoundRejection
import edazdarevic.commons.net.CIDRUtils

/**
  * Created by Ivan von Nagy on 1/19/15.
  */

trait CIDRDirectives extends BaseDirectives {

  def cidrFilter(allow: Seq[String], deny: Seq[String]): Directive0 = {
    lazy val allowed = allow map (x => new CIDRUtils(x.trim))
    lazy val denied = deny map (x => new CIDRUtils(x.trim))

    (optionalHeaderValuePF {
      case `X-Forwarded-For`(Seq(ip, _*)) => ip
      case `Remote-Address`(ip) => ip
      case `X-Real-Ip`(ip) => ip
    }) flatMap {
      case Some(ip) if ip.toOption.isDefined =>
        // Deny trumps allow

        denied.count(_.isInRange(ip.toOption.get.getHostAddress)) match {
          case 0 if allowed.length > 0 =>
            // No denies, but we must match against allows
            allowed.count(_.isInRange(ip.toOption.get.getHostAddress)) match {
              case 0 =>
                // Not explicitly denied, but no allow matches
                reject(NotFoundRejection("The requested resource could not be found"))
              //complete(StatusCodes.NotFound)
              case _ =>
                // Not denies and matches an allow
                pass
            }
          case 0 =>
            // No denies and no allows to match against
            pass
          case _ =>
            // Denied
            reject(NotFoundRejection("The requested resource could not be found"))
          //complete(StatusCodes.NotFound)
        }
      case _ =>
        // Denied because there is no remote address header that has been injected
        reject(NotFoundRejection("The requested resource could not be found"))
      //complete(StatusCodes.NotFound)
    }
  }

}