addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.8")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.17"

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.9.3")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.19.0")

addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.11.0")

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.3.2")

addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.21.1")

addSbtPlugin("com.thesamet" % "sbt-protoc-gen-project" % "0.1.8")
