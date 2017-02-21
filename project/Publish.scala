import sbt.Keys._
import sbt._


object Publish {

  lazy val settings = Seq[Def.Setting[_]](

    resolvers ++= Seq(
      Resolver.mavenLocal,
      Resolver.sonatypeRepo("releases"),
      Resolver.sonatypeRepo("snapshots")
    ),

    /* Packaging settings */
    exportJars := true,

    // Publishing settings
    publishMavenStyle := true,

    // Don't publish any of the test stuff
    publishArtifact in sbt.Test := false,

    // Maven central cannot allow other repos.
    pomIncludeRepository := { _ => false },

    /**
      * We load the Sonatype credentials from the current sbt version folder (e.g. ``~/.sbt/0.13/sonatype.sbt``) file. This file must look
      * like the following:
      * credentials += Credentials("Sonatype Nexus Repository Manager",
      * "oss.sonatype.org",
      * "<your username>",
      * "<your password>")
      */

    // When publishing remotely, use these settings
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },

    /**
      * We construct _proper_ Maven-esque POMs to be able to release on Maven.
      */
    pomExtra := (
      <url>https://github.com/vonnagy/service-container</url>
        <licenses>
          <license>
            <name>The Apache Software License, Version 2.0</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>git@github.com:vonnagy/service-container.git</url>
          <connection>scm:git@github.com:vonnagy/service-container.git</connection>
          <developerConnection>scm:git@github.com:vonnagy/service-container.git</developerConnection>
          <tag>HEAD</tag>
        </scm>
        <developers>
          <developer>
            <id>ivan.von.nagy</id>
            <name>Ivan von Nagy</name>
            <email>ivan@vonnagy.net</email>
          </developer>
        </developers>
      )


  )

}