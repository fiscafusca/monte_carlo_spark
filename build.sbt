name := "Giorgia_Fiscaletti_hw3"
version := "0.1"
scalaVersion := "2.11.8"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.6.4"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe" % "config" % "1.3.4"
libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.4.2"
libraryDependencies += "com.google.guava" % "guava" % "14.0.1"
libraryDependencies += "org.apache.spark" %% "spark-core" % "2.2.1" exclude("org.slf4j", "slf4j-log4j12") exclude("ch.qos.logback", "slf4j-log4j12") exclude("com.google.guava","guava")
libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.2.1" exclude("org.slf4j", "slf4j-log4j12") exclude("ch.qos.logback", "slf4j-log4j12") exclude("com.google.guava","guava")
libraryDependencies += "au.com.bytecode" % "opencsv" % "2.4"

mainClass in (Compile, run) := Some("com.gfisca2.Init")
mainClass in assembly := Some("com.gfisca2.Init")
assemblyJarName in assembly := "montecarlo.jar"
