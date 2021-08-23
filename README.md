[![Java CI with Gradle](https://github.com/scoverage/gradle-scoverage/actions/workflows/gradle.yml/badge.svg)](https://github.com/scoverage/gradle-scoverage/actions/workflows/gradle.yml)

gradle-scoverage
================
A plugin to enable the use of Scoverage in a gradle Scala project.

Usage
-----

You can find instructions on how to apply the plugin at http://plugins.gradle.org/plugin/org.scoverage

### Available tasks

1. `reportScoverage`: Produces XML and HTML reports for analysing test code coverage.

    The plugin automatically creates a `report{Task name}Scoverage` task for each test task in your
    Gradle build.  The `reportScoverage` task will run all test tasks and you can use the individual
    tasks to run only the desired tests.  For example, to run only the unit tests and no other test
    tasks (e.g., integration tests), you can run `reportTestScoverage`.

2. `aggregateScoverage`: Aggregates coverage statistics in composite builds.

    When applied on a project with sub-projects, the plugin will create the aggregation task `aggregateScoverage`, which
    will first generate reports for each project individually (including the parent project), and will then generate an
    aggregated result based on these reports.
    
    The plugin must be applied on a sub-project for it to be included in the aggregated; applying the plugin on a
    project _does not_ automatically apply it on sub-projects.   

    The aggregated report will override the parent-project specific report (`parent-project/build/reports/scoverage`).

    One can still use `reportScoverage` in order to generate a report without aggregation.

3. `checkScoverage`: Validates coverage status according to generated reports (aggregated or not).

    `gradle checkScoverage` will automatically invoke `reportScoverage` but it won't generate aggregated reports.
    In order to check coverage of aggregated reports one should use `gradle checkScoverage aggregateScoverage`.

**Note:** The plugin is not compatible with composite builds. For more information, see [the relevant issue](https://github.com/scoverage/gradle-scoverage/issues/98).
    
### Configuration

The plugin exposes multiple options that can be configured by setting them in an `scoverage` block within the project's
build script. These options are as follows:

* `scoverageVersion = <String>` (default `"1.4.8`): The version of the scoverage scalac plugin. This (gradle) plugin
should be compatible with all 1+ versions.

* `scoverageScalaVersion = <String>` (default `detected`): The scala version of the scoverage scalac plugin. This
overrides the version of the `scala-library` compile dependency (if the dependency is configured).
  
* `coverageOutputCobertura = <boolean>` (default `true`): Enables/disables cobertura.xml file generation (for both aggregated and non-aggregated reports).

* `coverageOutputXML = <boolean>` (default `true`): Enables/disables scoverage XML output (for both aggregated and non-aggregated reports).

* `coverageOutputHTML = <boolean>` (default `true`): Enables/disables scoverage HTML output (for both aggregated and non-aggregated reports).

* `coverageDebug = <boolean>` (default `false`): Enables/disables scoverage debug output (for both aggregated and non-aggregated reports).

* `minimumRate = <double>` (default `0.75`): The minimum amount of coverage in decimal proportion (`1.0` == 100%)
required for the validation to pass (otherwise `checkScoverage` will fail the build). 

* `coverageType = <CoverageType.Statement | CoverageType.Branch | CoverageType.Line>` (default `CoverageType.Statement`): The type of coverage validated by the
`checkScoverage` task. For more information on the different types, please refer to the documentation of the scalac
plugin (https://github.com/scoverage/scalac-scoverage-plugin).

#### Multiple check tasks

It is possible to configure multiple checks; for instance, one check for a statement rate and another for a branch rate:
```
scoverage {
    check {
        minimumRate = 0.5
        coverageType = CoverageType.Statement
    }
    check {
        minimumRate = 0.8
        coverageType = CoverageType.Branch
    }
}
```

Note that you cannot mix multiple-checks syntax with plain check configuration:
```
// ok
scoverage {
    check {
        minimumRate = 0.5
        coverageType = CoverageType.Statement
    }
}

// ok
scoverage {
    minimumRate = 0.2
}

// NOT ok
scoverage {
    minimumRate = 0.2
    check {
        minimumRate = 0.5
        coverageType = CoverageType.Statement
    }
}
``` 

### Run without normal compilation

By default, running any of the plugin tasks will compile the code both using "normal" compilation (`compileScala`)
and using the scoverage scalac plugin (`compileScoverageScala`).

In cases where you only wish to generate reports / validate coverage, but are not interested in publishing the code,
it is possible to only compile the code with the scoverage scalac plugin, thus reducing build times significantly.
In order to do so, simply add the arguments `-PscoverageCompileOnly` to the gradle execution.
For example: `gradle reportScoverage -PscoverageCompileOnly`.

Note that this mode is incompatible with parallel builds in multi-module projects.

### Compatibility with Consistent Versions Plugin

In order for the plugin to work alongside [Palantir's consistent versions plugin](https://github.com/palantir/gradle-consistent-versions),
the Scala version must be manually configured (via `scoverageScalaVersion`); otherwise, the plugin will attempt to
resolve the compilation classpath, which is prohibited by the versions plugin.

Migration to 7.x
----------------

* Running without normal compilation is now made with `-PscoverageCompileOnly` instead of `-x compileScala`.

Migration to 5.x
----------------

* Requires scoverage 1.4.2 or higher (and uses this version by default)
* Adds support for Scala 2.13
* Drops support for Scala 2.11

Migration to 4.x
----------------

* Requires scoverage 1.4.1 or higher (and uses this version by default)
* Requires application of the plugin to appropriate subprojects. A multi-module project might apply it to all.

```groovy
plugins {
    id 'org.scoverage' version '4.0.0'
}
subprojects {
    apply plugin: 'org.scoverage'
}
```

Migration to 3.x
----------------

* No more `testScoverage` task; instead, `test` will run coverage whenever the build is invoked with any of the scoverage tasks.

* No more need to declare scalac dependencies:
```groovy
// can safely delete this from build scripts
dependencies {
    scoverage group: 'org.scoverage', name: 'scalac-scoverage-plugin_2.12', version: '1.3.1'
    scoverage group: 'org.scoverage', name: 'scalac-scoverage-runtime_2.12', version: '1.3.1'
}
```

* All configurations are configured in `scoverage` block. For instance:
```groovy
// do this
scoverage {
    minimumRate = 0.5
}

// instead of this
checkScoverage {
    minimumRate = 0.5
}
```

* No more need to declare aggregation task:
```groovy
// can safely delete this from build scripts
task aggregateScoverage(type: org.scoverage.ScoverageAggregate)
checkScoverage {
     reportDir = file("$buildDir/scoverage-aggregate")
}
```