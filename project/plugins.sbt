addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.0-RC7-1")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.9"

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")

addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.4")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.3.1")

addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.7.0")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")

addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.20.0")

addSbtPlugin("com.thesamet" % "sbt-protoc-gen-project" % "0.1.6")
