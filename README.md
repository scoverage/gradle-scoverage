[![Build Status](https://travis-ci.org/scoverage/gradle-scoverage.png?branch=master)](https://travis-ci.org/scoverage/gradle-scoverage)

gradle-scoverage
================
A plugin to enable the use of Scoverage in a gradle Scala project.

This has now been deployed to maven central.

Getting started
---------------
```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'org.scoverage:gradle-scoverage:0.4.1'
    }
}

apply plugin: scoverage.ScoveragePlugin

dependencies {
    scoverage 'org.scoverage:scalac-scoverage-plugin_2.10:0.98.4'
    compile 'org.scala-lang:scala-library:2.10.1'
}
```

This creates an additional task testCoverage which will run tests against instrumented code

- [x] instrumenting main scala code
- [x] running JUnit tests against instrumented scala code
- [x] failing the build on lack of coverage

Then launch command :
`gradle testScoverage` or `gradle checkScoverage`


CheckScoverage
---------

By default, when you launch `gradle checkScoverage` build fail if only 75% of project is covered by tests.

To configure it as you want, add this configuration :
```
checkScoverage {
    minimumLineRate = 0.5
}
```
