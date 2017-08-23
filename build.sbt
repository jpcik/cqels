
name := "cqels"
organization := "rsp"
version := "1.2.2"
scalaVersion := "2.12.3"

//enablePlugins(JavaAppPackaging)
  
libraryDependencies ++= Seq(
  "org.apache.jena" % "apache-jena-libs" % "3.0.1" exclude("log4j","log4j") exclude("slf4j-api","org.slf4j"),
  "com.sleepycat" % "je" % "5.0.73",
  "com.espertech" % "esper" % "4.2.0" ,
  "com.jayway.awaitility" % "awaitility" % "1.6.0",
  //"org.linkeddatafragments" % "ldf-client" % "0.1.1-SNAPSHOT",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "junit" % "junit" % "4.12" % "test"
)

resolvers ++= Seq(
  "typesafe" at "http://repo.typesafe.com/typesafe/releases/",
  "lof" at "https://raw.github.com/semiotproject/Client.Java/mvn-repo/",
  "plord" at "http://homepages.cs.ncl.ac.uk/phillip.lord/maven"
)

//mainClass in Compile := Some("rsp.engine.rewriting.Experiments")

//scriptClasspath := Seq("*")

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

scalacOptions ++= Seq("-feature","-deprecation")

publishArtifact in (Compile, packageDoc) := false
