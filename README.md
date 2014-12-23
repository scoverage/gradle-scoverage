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
        classpath 'org.scoverage:gradle-scoverage:1.0-5-g9c68988'
    }
}

apply plugin: 'scoverage'

dependencies {
    scoverage 'org.scoverage:scalac-scoverage-plugin_2.11:1.0.2', 'org.scoverage:scalac-scoverage-runtime_2.11:1.0.2'
}
```

This creates an additional task testCoverage which will run tests against instrumented code

- [x] instrumenting main scala code
- [x] running JUnit tests against instrumented scala code
- [x] failing the build on lack of coverage

Then launch command :
`gradle testScoverage` or `gradle checkScoverage`

Available tasks
---------
* testScoverage - Executes all tests and creates Scoverage XML report with information about code coverage
* reportScoverage - Generates HTML report.
* checkScoverage - See below.
* compileScoverageScala - Instruments code without running tests.

CheckScoverage
---------

By default, when you launch `gradle checkScoverage` build fail if only 75% of project is covered by tests.

To configure it as you want, add this configuration :
```
checkScoverage {
    minimumLineRate = 0.5
}
```
