ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.32")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.4"

val validateVersion = "0.0.0+35-3753c2ae-SNAPSHOT"

libraryDependencies += "com.thesamet.scalapb" %% "scalapb-validate-codegen" % validateVersion
