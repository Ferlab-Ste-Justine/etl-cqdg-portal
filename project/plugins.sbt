logLevel := Level.Info

addSbtPlugin("org.jetbrains" % "sbt-ide-settings" % "1.1.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")

resolvers += Resolver.sbtPluginRepo("releases")
resolvers += Resolver.mavenCentral
resolvers += Resolver.typesafeIvyRepo("releases")