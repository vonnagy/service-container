resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")

// Add the pgp plugin
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0")

// Add the release plugin
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.7")

// Add the Scalastyle plugin for reviewing code
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.7.0")

// Add the scoverage plugin for code coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

// Add coveralls plugin for historical code coverage
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.2.2")

// This plugin is is for looking at the dependencies
addSbtPlugin("com.gilt" % "sbt-dependency-graph-sugar" % "0.9.0")