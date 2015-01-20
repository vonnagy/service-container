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

  // The config at 'container.http'
  implicit val config: Config
  private lazy val allow = immutableSeq(config.getStringList("cidr.allow")) map (x => new CIDRUtils(x.trim))
  private lazy val deny = immutableSeq(config.getStringList("cidr.deny")) map (x => new CIDRUtils(x.trim))

  def cidrFilter: Directive0 = {
    (optionalHeaderValuePF {
      case `Remote-Address`(ip) => ip
    }) flatMap {
      case Some(ip) if ip.toOption.isDefined =>
        // Deny trumps allow

        deny.count(_.isInRange(ip.toOption.get.getHostAddress)) match {
          case 0 if allow.length > 0 =>
            // No denies, but we must match against allows
            allow.count(_.isInRange(ip.toOption.get.getHostAddress)) match {
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