[![Build Status](https://travis-ci.org/scoverage/gradle-scoverage.png?branch=master)](https://travis-ci.org/scoverage/gradle-scoverage)

gradle-scoverage
================
A plugin to enable the use of Scoverage in a gradle Scala project.

Getting started
---------------

http://plugins.gradle.org/plugin/org.scoverage

Available tasks
---------------

1. `reportScoverage`: Produces XML and HTML reports for analysing test code coverage.

2. `aggregateScoverage`: An experimental support for aggregating coverage statistics in composite builds.

    When applied on a project with sub-projects, the plugin will create the aggregation task `aggregateScoverage`, which
    will first generate reports for each project individually (including the parent project), and will then generate an
    aggregated result based on these reports.

    The aggregated report will override the parent-project specific report (`parent-project/build/reports/scoverage`).

    One can still use `reportScoverage` in order to generate a report without aggregation.

3. `checkScoverage`: Validates coverage according status according the generated reports (aggregated or not).

    `gradle checkScoverage` will automatically invoke `reportScoverage` but it won't generate aggregated reports.
    In order to check coverage of aggregated reports one should use `gradle checkScoverage aggregateScoverage`.
    
Configuration
---------------

The plugin exposes multiple options that can be configured by setting them in an `scoverage` block within the project's
build script. These options are as follows:

You can configure the version of Scoverage that will be used. This plugin should 

* `scoverageVersion = <String>` (default `"1.3.1"`): The version of the scoverage scalac plugin. This (gradle) plugin
should be compatible with all 1+ versions.

* `scoverageScalaVersion = <String>` (default `"2.12"`): The scala version of the scoverage scalac plugin. This will
be overridden by the version of the `scala-library` compile dependency (if the dependency is configured).
  
* `coverageOutputCobertura = <boolean>` (default `true`): Enables/disables cobertura.xml file generation (for both aggregated and non-aggregated reports).

* `coverageOutputXML = <boolean>` (default `true`): Enables/disables scoverage XML output (for both aggregated and non-aggregated reports).

* `coverageOutputHTML = <boolean>` (default `true`): Enables/disables scoverage HTML output (for both aggregated and non-aggregated reports).

* `coverageDebug = <boolean>` (default `false`): Enables/disables scoverage debug output (for both aggregated and non-aggregated reports).

* `minimumRate = <double>` (default `0.75`): The minimum amount of coverage in decimal proportion (`1.0` == 100%)
required for the validation to pass (otherwise `checkScoverage` will fail the build). 

* `coverageType = <"Statement" | "Branch" | "Line">` (default `"Statement"`): The type of coverage validated by the
`checkScoverage` task. For more information on the different types, please refer to the documentation of the scalac
plugin (https://github.com/scoverage/scalac-scoverage-plugin).

* `runNormalCompilation = <boolean>` (default `true`): Determines whether both normal scalac compilation (`compileScala`) 
and compilation with scoverage (`compileScoverageScala`) should be executed, or if only the scoverage compilation should.
It may be helpful to turn this off so that only the scoverage instrumented classes -- which are not intended for release
-- will be created, thus reducing the build time.


