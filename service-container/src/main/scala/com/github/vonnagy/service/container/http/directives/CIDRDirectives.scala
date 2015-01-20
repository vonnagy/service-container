package com.github.vonnagy.service.container.http.directives

import akka.japi.Util.immutableSeq
import com.github.vonnagy.service.container.http.BaseDirectives
import com.typesafe.config.Config
import edazdarevic.commons.net.CIDRUtils
import spray.http.HttpHeaders.`Remote-Address`
import spray.http.StatusCodes
import spray.routing.Directive0


/**
 * Created by Ivan von Nagy on 1/19/15.
 */

trait CIDRDirectives extends BaseDirectives {

  def cidrFilter(allow: Seq[String], deny: Seq[String]): Directive0 = {
    lazy val allowed = allow map (x => new CIDRUtils(x.trim))
    lazy val denied = deny map (x => new CIDRUtils(x.trim))

    (optionalHeaderValuePF {
      case `Remote-Address`(ip) => ip
    }) flatMap {
      case Some(ip) if ip.toOption.isDefined =>
        // Deny trumps allow

        denied.count(_.isInRange(ip.toOption.get.getHostAddress)) match {
          case 0 if allow.length > 0 =>
            // No denies, but we must match against allows
            allowed.count(_.isInRange(ip.toOption.get.getHostAddress)) match {
              case 0 =>
                // Not explicitly denied, but no allow matches
                complete(StatusCodes.NotFound)
              case _ =>
                // Not denies and matches an allow
                pass
            }
          case 0 =>
            // No denies and no allows to match against
            pass
          case _ =>
            // Denied
            complete(StatusCodes.NotFound)
        }
      case _ =>
        // Denied because there is no remote address header that has been injected
        complete(StatusCodes.NotFound)
    }
  }

}