name := "pictblizzard"

version := "0.0.1"

organization := "com.github.chuwb"

scalaVersion := "2.10.0"

//parallelExecution in Test := false

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "com.twitter" % "util-logging" % "5.3.12",
  "net.sf.opencsv" % "opencsv" % "2.0"
  //"swt" % "jface" % "3.0.1"
)

// reduce the maximum number of errors shown by the Scala compiler
maxErrors := 20

// increase the time between polling for file changes when using continuous execution
pollInterval := 1000

// set the prompt (for this build) to include the project id.
//shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }

// disable printing timing information, but still print [success]
showTiming := true

// disable printing a message indicating the success or failure of running a task
showSuccess := true

// only show 10 lines of stack traces
traceLevel := 10

// add SWT to the unmanaged classpath
//unmanagedJars in Compile += file("/usr/share/java/swt.jar")

