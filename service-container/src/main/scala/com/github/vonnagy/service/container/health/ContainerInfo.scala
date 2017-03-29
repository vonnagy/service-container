package com.github.vonnagy.service.container.health

import java.net.InetAddress
import java.util.jar.Attributes.Name
import java.util.jar.{Attributes, JarFile, Manifest}

import com.github.vonnagy.service.container.log.LoggingAdapter

/**
 * Created by Ivan von Nagy on 1/12/15.
 */
object ContainerInfo extends LoggingAdapter {

  private val mainClass = getMainClass;
  private val applicationInfo = getApplicationInfo

  val scalaVersion = util.Properties.versionString
  val host = getHostInternal
  val application = applicationInfo._1
  val applicationVersion = applicationInfo._2

  val containerManifest = getManifest(this.getClass)
  val containerVersion = containerManifest.getMainAttributes().getValue("Implementation-Version") + "." + containerManifest.getMainAttributes().getValue("Implementation-Build")

  /**
   * Get the system host
   * @return the host name
   */
  private[health] def getHostInternal: String = {
    try {
      InetAddress.getLocalHost.getHostName.split("\\.")(0)
    }
    catch {
      case ex: Exception => {
        "Unknown"
      }
    }
  }

  /**
   * Get the name and version information for the application
   * @return
   */
  private[health] def getApplicationInfo: Tuple2[String, String] = {
    if (mainClass.isDefined) {
      val man: Manifest = getManifest(mainClass.get)
      Tuple2[String, String](man.getMainAttributes.getValue(Name.IMPLEMENTATION_TITLE),
        man.getMainAttributes.getValue("Implementation-Version") + "." +
          man.getMainAttributes.getValue("Implementation-Build"))
    }
    else {
      Tuple2[String, String]("Container Service", "1.0.0.N/A")
    }
  }

  /**
   * Find the main class that is the entry point
   * @return
   */
  private[health] def getMainClass: Option[Class[_]] = {
    import scala.collection.JavaConverters._

    def checkStack(elem: StackTraceElement): Option[Class[_]] = try {
      if (elem.getMethodName.equals("main")) Some(Class.forName(elem.getClassName)) else None
    } catch {
      case e: ClassNotFoundException => {
        // Swallow the exception
        None
      }
    }

    Thread.getAllStackTraces.asScala.values.flatMap(currStack => {
      if (!currStack.isEmpty)
        checkStack(currStack.last)
      else
        None
    }).headOption match {
      case None =>
        sys.props.get("sun.java.command") match {
          case Some(command) if !command.isEmpty =>
            try {
              Some(Class.forName(command))
            } catch {
              // Swallow the exception
              case e: ClassNotFoundException =>
                None
            }

          // Nothing could be located
          case _ => None
        }
      case c => c
    }
  }

  private[health] def getManifest(clazz: Class[_]): Manifest = {
    val file: String = clazz.getProtectionDomain.getCodeSource.getLocation.getFile

    try {
      if (file.endsWith(".jar")) {
        new JarFile(file).getManifest
      }
      else {
        val manifest: Manifest = new Manifest
        manifest.getMainAttributes.put(Name.IMPLEMENTATION_TITLE, "Container Service")
        manifest.getMainAttributes.put(Name.IMPLEMENTATION_VERSION, "1.0.0")
        manifest.getMainAttributes.put(new Attributes.Name("Implementation-Build"), "N/A")
        manifest
      }
    }
    catch {
      case e: Exception => {
        val manifest: Manifest = new Manifest
        manifest.getMainAttributes.put(Name.IMPLEMENTATION_TITLE, "Container Service")
        manifest.getMainAttributes.put(Name.IMPLEMENTATION_VERSION, "1.0.0")
        manifest.getMainAttributes.put(new Attributes.Name("Implementation-Build"), "N/A")
        manifest
      }
    }
  }
}
