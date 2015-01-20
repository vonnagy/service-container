package com.github.vonnagy.service.container.health

import java.net.InetAddress
import java.util.jar.Attributes.Name
import java.util.jar.{Attributes, JarFile, Manifest}

import akka.japi.Option
import com.github.vonnagy.service.container.log.LoggingAdapter

/**
 * Created by Ivan von Nagy on 1/12/15.
 */
object ContainerInfo extends LoggingAdapter {

  val scalaVersion = util.Properties.versionString

  val host = getHostInternal
  val mainClass = getMainClass;
  val applicationInfo = getApplicationInfo
  val application = applicationInfo._1
  val applicationVersion = applicationInfo._2

  val manifest = getManifest(this.getClass)
  val containerVersion = manifest.getMainAttributes().getValue("Implementation-Version") + "." + manifest.getMainAttributes().getValue("Implementation-Build")

  /**
   * Get the system host
   * @return the host name
   */
  private[health] def getHostInternal: String = {
    try {
      return InetAddress.getLocalHost.getHostName.split("\\.")(0)
    }
    catch {
      case ex: Exception => {
      }
    }
    return "Unknown"
  }

  /**
   * Get the name and version information for the application
   * @return
   */
  private[health] def getApplicationInfo: Tuple2[String, String] = {
    if (mainClass.isDefined) {
      val man: Manifest = getManifest(mainClass.get)
      return Tuple2[String, String](man.getMainAttributes.getValue(Name.IMPLEMENTATION_TITLE), man.getMainAttributes.getValue("Implementation-Version") + "." + man.getMainAttributes.getValue("Implementation-Build"))
    }
    else {
      return Tuple2[String, String]("Container Service", "N/A")
    }
  }

  /**
   * Find the main class that is the entry point
   * @return
   */
  private[health] def getMainClass: Option[Class[_]] = {
    import scala.collection.JavaConversions._

    for (currStack <- Thread.getAllStackTraces.values if currStack.length > 0) {
      val lastElem = currStack(currStack.length - 1)
      if (lastElem.getMethodName == "main") {
        try {
          return Option.some(Class.forName(lastElem.getClassName))
        }
        catch {
          case e: ClassNotFoundException => {
            e.printStackTrace
          }
        }
      }
    }
    return Option.none
  }

  private[health] def getManifest(clazz: Class[_]): Manifest = {
    val file: String = clazz.getProtectionDomain.getCodeSource.getLocation.getFile

    try {
      if (file.endsWith(".jar")) {
        return new JarFile(file).getManifest
      }
      else {
        val manifest: Manifest = new Manifest
        manifest.getMainAttributes.put(Name.IMPLEMENTATION_TITLE, "Container Service")
        manifest.getMainAttributes.put(Name.IMPLEMENTATION_VERSION, "1.0.0")
        manifest.getMainAttributes.put(new Attributes.Name("Implementation-Build"), "N/A")
        return manifest
      }
    }
    catch {
      case e: Exception => {
        val manifest: Manifest = new Manifest
        manifest.getMainAttributes.put(Name.IMPLEMENTATION_TITLE, "Container Service")
        manifest.getMainAttributes.put(Name.IMPLEMENTATION_VERSION, "1.0.0")
        manifest.getMainAttributes.put(new Attributes.Name("Implementation-Build"), "N/A")
        log.error("Could not fetch the manifest", e)
        return manifest
      }
    }
  }
}
