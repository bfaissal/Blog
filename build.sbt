

name := """montessori"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

resolvers += "typesafe snapshots" at "https://repo.typesafe.com/typesafe/snapshots"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.AKKA23",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23")

herokuAppName in Compile := "arabicmontessori"

herokuProcessTypes in Compile := Map(
  "web" -> "target/universal/stage/bin/montessori -Dhttp.port=${PORT} -Dmongodb.uri=$MONGO_URL -DOPENSHIFT_GRID_MAIL_USER=$OPENSHIFT_GRID_MAIL_USER -DOPENSHIFT_GRID_MAIL_password=$OPENSHIFT_GRID_MAIL_password"
)