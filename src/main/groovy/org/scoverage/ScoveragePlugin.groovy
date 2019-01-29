package org.scoverage

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.PluginAware
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.util.GFileUtils

import java.util.concurrent.Callable

class ScoveragePlugin implements Plugin<PluginAware> {

    static final String CONFIGURATION_NAME = 'scoverage'
    static final String TEST_NAME = 'testScoverage'
    static final String REPORT_NAME = 'reportScoverage'
    static final String CHECK_NAME = 'checkScoverage'
    static final String COMPILE_NAME = 'compileScoverageScala'
    static final String AGGREGATE_NAME = 'aggregateScoverage'

    @Override
    void apply(PluginAware pluginAware) {
        if (pluginAware instanceof Project) {
            applyProject(pluginAware)
            if (pluginAware == pluginAware.rootProject) {
                pluginAware.subprojects { p ->
                    p.plugins.apply(ScoveragePlugin)
                }
            }
        } else if (pluginAware instanceof Gradle) {
            pluginAware.allprojects { p ->
                p.plugins.apply(ScoveragePlugin)
            }
        } else {
            throw new IllegalArgumentException("${pluginAware.getClass()} is currently not supported as an apply target, please report if you need it")
        }
    }

    void applyProject(Project project) {

        if (project.plugins.hasPlugin(ScoveragePlugin)) {
            project.logger.info("Project ${project.name} already has the scoverage plugin")
            return
        }
        project.logger.info("Applying scoverage plugin to $project.name")

        def extension = project.extensions.create('scoverage', ScoverageExtension, project)
        if (!project.configurations.asMap[CONFIGURATION_NAME]) {
            project.configurations.create(CONFIGURATION_NAME) {
                visible = false
                transitive = true
                description = 'Scoverage dependencies'
            }

            project.afterEvaluate {
                def scoverageVersion = project.extensions.scoverage.scoverageVersion.get()
                def scalaVersion = project.extensions.scoverage.scoverageScalaVersion.get()

                def scalaLibrary = project.configurations.compile.dependencies.find {
                    it.group == "org.scala-lang" && it.name == "scala-library"
                }

                if (scalaLibrary != null) {
                    scalaVersion = scalaLibrary.version.substring(0, scalaLibrary.version.lastIndexOf("."))
                }

                def fullScoverageVersion = "$scalaVersion:$scoverageVersion"

                project.logger.info("Using scoverage scalac plugin version '$fullScoverageVersion'")

                project.dependencies {
                    scoverage("org.scoverage:scalac-scoverage-plugin_$fullScoverageVersion")
                    scoverage("org.scoverage:scalac-scoverage-runtime_$fullScoverageVersion")
                }
            }
        }

        ScoverageRunner scoverageRunner = new ScoverageRunner(project.configurations.scoverage)

        createTasks(project, extension, scoverageRunner)

        project.afterEvaluate {
            configureAfterEvaluation(project, extension, scoverageRunner)
        }
    }

    private void createTasks(Project project, ScoverageExtension extension, ScoverageRunner scoverageRunner) {


        def instrumentedSourceSet = project.sourceSets.create('scoverage') {
            def original = project.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)

            resources.source(original.resources)
            scala.source(original.java)
            scala.source(original.scala)

            compileClasspath += original.compileClasspath + project.configurations.scoverage
            runtimeClasspath = it.output + project.configurations.scoverage + original.runtimeClasspath
        }

        def scoverageJar = project.tasks.create('jarScoverage', Jar.class) {
            dependsOn('scoverageClasses')
            classifier = CONFIGURATION_NAME
            from instrumentedSourceSet.output
        }
        project.artifacts {
            scoverage scoverageJar
        }

        project.tasks.create(TEST_NAME, Test.class) {
            conventionMapping.map("classpath", new Callable<Object>() {
                Object call() throws Exception {
                    def testSourceSet = project.sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)
                    return testSourceSet.output +
                            instrumentedSourceSet.output +
                            project.configurations.scoverage +
                            testSourceSet.runtimeClasspath
                }
            })
            group = 'verification'

            FilenameFilter measurementFile = new FilenameFilter() {
                @Override
                boolean accept(File dir, String name) {
                    return name.startsWith("scoverage.measurements.")
                }
            }
            outputs.upToDateWhen { extension.dataDir.get().listFiles(measurementFile) }
        }

        project.tasks.create(REPORT_NAME, ScoverageReport.class) {
            dependsOn(project.tasks[TEST_NAME])
            onlyIf { extension.dataDir.get().list() }
            group = 'verification'
            runner = scoverageRunner
            reportDir = extension.reportDir
            sources = extension.sources
            dataDir = extension.dataDir
            coverageOutputCobertura = extension.coverageOutputCobertura
            coverageOutputXML = extension.coverageOutputXML
            coverageOutputHTML = extension.coverageOutputHTML
            coverageDebug = extension.coverageDebug
        }

        project.tasks.create(CHECK_NAME, OverallCheckTask.class) {
            dependsOn(project.tasks[REPORT_NAME])
            group = 'verification'
            reportDir = extension.reportDir
        }

    }

    private void configureAfterEvaluation(Project project, ScoverageExtension extension, ScoverageRunner scoverageRunner) {

        if (project.childProjects.size() > 0) {
            def reportTasks = project.getSubprojects().collect { it.tasks.withType(ScoverageReport) }
            project.tasks.create(AGGREGATE_NAME, ScoverageAggregate.class) {
                dependsOn(reportTasks)
                group = 'verification'
                runner = scoverageRunner
                coverageOutputCobertura = extension.coverageOutputCobertura
                coverageOutputXML = extension.coverageOutputXML
                coverageOutputHTML = extension.coverageOutputHTML
                coverageDebug = extension.coverageDebug
            }
        }

        project.tasks[COMPILE_NAME].configure {
            File pluginFile = project.configurations[CONFIGURATION_NAME].find {
                it.name.startsWith("scalac-scoverage-plugin")
            }
            List<String> parameters = ['-Xplugin:' + pluginFile.absolutePath]
            List<String> existingParameters = scalaCompileOptions.additionalParameters
            if (existingParameters) {
                parameters.addAll(existingParameters)
            }
            parameters.add("-P:scoverage:dataDir:${extension.dataDir.get().absolutePath}".toString())
            if (extension.excludedPackages.get()) {
                def packages = extension.excludedPackages.get().join(';')
                parameters.add("-P:scoverage:excludedPackages:$packages".toString())
            }
            if (extension.excludedFiles.get()) {
                def packages = extension.excludedFiles.get().join(';')
                parameters.add("-P:scoverage:excludedFiles:$packages".toString())
            }
            if (extension.highlighting.get()) {
                parameters.add('-Yrangepos')
            }
            doFirst {
                GFileUtils.deleteDirectory(destinationDir)
            }
            scalaCompileOptions.additionalParameters = parameters
            // the compile task creates a store of measured statements
            outputs.file(new File(extension.dataDir.get(), 'scoverage.coverage.xml'))
        }
    }
}