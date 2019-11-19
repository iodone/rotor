
name := "rotor"
scalaVersion in ThisBuild := "2.11.8"

val sparkVersion = "2.3.2"

lazy val global = project
  .in(file("."))
  .aggregate(
    common,
    spark,
    engine,
    proxy
  )

lazy val common = project
  .settings(
    name := "rotor-common",
    libraryDependencies ++= commonDependencies
  )

lazy val spark = project
  .settings(
    name := "rotor-spark",
    libraryDependencies ++= sparkDependencies ++ utilsDependencies
  )

lazy val engine = project
  .settings(
    name := "rotor-engine",
    assemblySettings,
    dependencyOverrides += "org.lz4" % "lz4" % "1.3.0" % "provided",
    libraryDependencies ++= commonDependencies ++ sparkDependencies ++ utilsDependencies ++ Seq(
      "com.lihaoyi" %% "fastparse" % "2.1.0"
    )
  )
  .dependsOn(
    common,
    spark
  )

lazy val proxy = project
  .settings(
    name := "rotor-proxy",
    assemblySettings,
    excludeDependencies ++= Seq(
      // commons-logging is replaced by jcl-over-slf4j
      ExclusionRule("org.slf4j", "slf4j-log4j12")
    ),
    libraryDependencies ++=  proxyDependencies ++ commonDependencies ++ Seq(
    )
  )
  .dependsOn(
    common
  )

lazy val sparkDependencies = Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVersion  % "provided",
  "org.apache.spark" %% "spark-hive" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-hive-thriftserver" % sparkVersion % "provided",
  "org.apache.hbase" % "hbase-common" % "1.2.0" % "provided",
  "org.apache.hbase" % "hbase-client" % "1.2.0" % "provided",
  "org.apache.hbase" % "hbase-server" % "1.2.0" % "provided",
  "org.elasticsearch" % "elasticsearch-hadoop" % "6.3.0" % "provided"
)

lazy val utilsDependencies = Seq(
  "net.liftweb" %% "lift-json" % "3.2.0",
  "org.keedio.openx.data" % "json-serde" % "1.3.7.3",
  "ru.yandex.clickhouse" % "clickhouse-jdbc" % "0.1.52"
  exclude("com.fasterxml.jackson.core", "com.fasterxml.jackson.core")
  exclude("com.fasterxml.jackson.core", "jackson-databind")
  exclude("org.apache.httpcomponents", "httpclient")
  exclude("org.apache.httpcomponents", "httpmime")
  exclude("com.google.guava", "guava"),
  "com.google.guava" % "guava" % "11.0.2",
  "log4j" % "log4j" % "1.2.17" % "provided"
)

lazy val proxyDependencies = Seq(
  // Config file parser
  // Sugar for serialization and deserialization in akka-http with circe
  "de.heikoseeberger" %% "akka-http-circe" % "1.20.1",

  // Validation library
  "com.wix" %% "accord-core" % "0.7.2",

  // Use for logging
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",

  // For validation
  //"org.squbs" %% "squbs-pattern" % "0.11.0",
  "com.wix" %% "accord-core" % "0.7.2",

  // For http client
  "com.softwaremill.sttp" %% "core" % "1.4.2",
  "com.softwaremill.sttp" %% "akka-http-backend" % "1.4.2",
  "com.softwaremill.sttp" %% "circe" % "1.4.2",

  // scala-async
  "org.scala-lang.modules" %% "scala-async" % "0.9.7",

  "org.scalamock" %% "scalamock" % "4.1.0" % Test,

  // Mock for test
  "org.mockito" % "mockito-all" % "1.9.5" % Test,

  // "ch.qos.logback" % "logback-classic" % "1.2.3"
  "ch.qos.logback" % "logback-classic" % "1.2.3"

)

lazy val commonDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.21",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.21" % Test,
  "com.typesafe.akka" %% "akka-remote" % "2.5.21",
  "com.typesafe.akka" %% "akka-http" % "10.1.7",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.7" % Test,
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",

  "mysql" % "mysql-connector-java" % "5.1.22" % "provided",

  //zk 组件
  "org.apache.curator" % "curator-recipes" % "2.11.1",
  "org.apache.curator" % "curator-client" % "2.11.1",
  "org.apache.curator" % "curator-framework" % "2.11.1",

  // Mock for unit test
  "org.mockito" % "mockito-all" % "1.9.5" % Test,

  // JSON serialization library
  "io.circe" %% "circe-core" % "0.10.0",
  "io.circe" %% "circe-generic" % "0.10.0",
  "io.circe" %% "circe-parser" % "0.10.0",
  "io.circe" %% "circe-optics" % "0.10.0",
  // for app conf
  "com.github.pureconfig" %% "pureconfig" % "0.9.2"

)

lazy val assemblySettings = Seq(
  assemblyJarName in assembly := s"${name.value}-${(version in ThisBuild).value}.jar",
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case "reference.conf" => MergeStrategy.concat
    case _ => MergeStrategy.first
  },
  assemblyExcludedJars in assembly := {
    val cp = (fullClasspath in assembly).value
    cp filter {_.data.getName == "apache-carbondata-1.5.0-bin-spark2.3.2-hadoop2.7.2.jar"}
  }
)
