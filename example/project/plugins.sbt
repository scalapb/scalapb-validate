ThisBuild / resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.34")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.7"

val validateVersion = "0.1.1"

libraryDependencies += "com.thesamet.scalapb" %% "scalapb-validate-codegen" % validateVersion
