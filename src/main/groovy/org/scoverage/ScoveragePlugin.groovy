package org.scoverage

import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.PluginAware
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.testing.Test

import java.nio.file.Files

import static groovy.io.FileType.FILES

class ScoveragePlugin implements Plugin<PluginAware> {

    static final String CONFIGURATION_NAME = 'scoverage'
    static final String REPORT_NAME = 'reportScoverage'
    static final String CHECK_NAME = 'checkScoverage'
    static final String COMPILE_NAME = 'compileScoverageScala'
    static final String AGGREGATE_NAME = 'aggregateScoverage'

    static final String DEFAULT_REPORT_DIR = 'reports' + File.separatorChar + 'scoverage'

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
                def scalaVersion = null

                def scalaLibrary = project.configurations.compile.dependencies.find {
                    it.group == "org.scala-lang" && it.name == "scala-library"
                }

                if (scalaLibrary != null) {
                    scalaVersion = scalaLibrary.version
                }

                if (scalaVersion == null && project.pluginManager.hasPlugin("io.spring.dependency-management")) {
                    scalaVersion = project.dependencyManagement.compile.managedVersions["org.scala-lang:scala-library"]
                }

                if (scalaVersion == null) {
                    scalaVersion = project.extensions.scoverage.scoverageScalaVersion.get()
                } else {
                    scalaVersion = scalaVersion.substring(0, scalaVersion.lastIndexOf("."))
                }

                def fullScoverageVersion = "$scalaVersion:$scoverageVersion"

                project.logger.info("Using scoverage scalac plugin version '$fullScoverageVersion'")

                project.dependencies {
                    scoverage("org.scoverage:scalac-scoverage-plugin_$fullScoverageVersion")
                    scoverage("org.scoverage:scalac-scoverage-runtime_$fullScoverageVersion")
                }
            }
        }

        createTasks(project, extension)
    }

    private void createTasks(Project project, ScoverageExtension extension) {

        ScoverageRunner scoverageRunner = new ScoverageRunner(project.configurations.scoverage)

        def originalSourceSet = project.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        def instrumentedSourceSet = project.sourceSets.create('scoverage') {

            resources.source(originalSourceSet.resources)
            scala.source(originalSourceSet.scala)

            compileClasspath += originalSourceSet.compileClasspath + project.configurations.scoverage
            runtimeClasspath = it.output + project.configurations.scoverage + originalSourceSet.runtimeClasspath
        }

        def originalCompileTask = project.tasks[originalSourceSet.getCompileTaskName("scala")]
        def originalJarTask = project.tasks[originalSourceSet.getJarTaskName()]

        def compileTask = project.tasks[instrumentedSourceSet.getCompileTaskName("scala")]
        compileTask.mustRunAfter(originalCompileTask)
        originalJarTask.mustRunAfter(compileTask)

        def globalReportTask = project.tasks.register(REPORT_NAME, ScoverageAggregate)
        def globalCheckTask = project.tasks.register(CHECK_NAME, OverallCheckTask)

        project.afterEvaluate {
            def testTasks = project.tasks.withType(Test)

            List<ScoverageReport> reportTasks = testTasks.collect { testTask ->
                testTask.mustRunAfter(compileTask)

                def reportTaskName = "report${testTask.name.capitalize()}Scoverage"
                def taskReportDir = project.file("${project.buildDir}/reports/scoverage${testTask.name.capitalize()}")

                project.tasks.create(reportTaskName, ScoverageReport) {
                    dependsOn compileTask, testTask
                    onlyIf { extension.dataDir.get().list() }
                    group = 'verification'
                    runner = scoverageRunner
                    reportDir = taskReportDir
                    sources = extension.sources
                    dataDir = extension.dataDir
                    coverageOutputCobertura = extension.coverageOutputCobertura
                    coverageOutputXML = extension.coverageOutputXML
                    coverageOutputHTML = extension.coverageOutputHTML
                    coverageDebug = extension.coverageDebug
                }
            }

            globalReportTask.configure {
                def reportDirs = reportTasks.findResults { it.reportDir.get() }

                dependsOn reportTasks
                onlyIf { reportDirs.any { it.list() } }

                group = 'verification'
                runner = scoverageRunner
                reportDir = extension.reportDir
                dirsToAggregateFrom = reportDirs
                deleteReportsOnAggregation = false
                coverageOutputCobertura = extension.coverageOutputCobertura
                coverageOutputXML = extension.coverageOutputXML
                coverageOutputHTML = extension.coverageOutputHTML
                coverageDebug = extension.coverageDebug
            }


            globalCheckTask.configure {
                dependsOn globalReportTask

                onlyIf { extension.reportDir.get().list() }
                group = 'verification'
                coverageType = extension.coverageType
                minimumRate = extension.minimumRate
                reportDir = extension.reportDir
            }

            // define aggregation task
            if (project.childProjects.size() > 0) {
                def allReportTasks = project.getAllprojects().findResults {
                    it.tasks.find { task ->
                        task.name == REPORT_NAME && task instanceof ScoverageAggregate
                    }
                }

                def aggregationTask = project.tasks.create(AGGREGATE_NAME, ScoverageAggregate) {
                    dependsOn(allReportTasks)
                    group = 'verification'
                    runner = scoverageRunner
                    reportDir = extension.reportDir
                    deleteReportsOnAggregation = extension.deleteReportsOnAggregation
                    coverageOutputCobertura = extension.coverageOutputCobertura
                    coverageOutputXML = extension.coverageOutputXML
                    coverageOutputHTML = extension.coverageOutputHTML
                    coverageDebug = extension.coverageDebug
                }
                project.tasks[CHECK_NAME].mustRunAfter(aggregationTask)
            }

            // make this project's scoverage compilation depend on scoverage compilation of any other project
            // which this project depends on its normal compilation
            // (essential when running without normal compilation on multi-module projects with inner dependencies)
            def originalCompilationDependencies = recursiveDependenciesOf(compileTask).findAll {
                it instanceof ScalaCompile
            }
            originalCompilationDependencies.each {
                def dependencyProjectCompileTask = it.project.tasks[COMPILE_NAME]
                def dependencyProjectReportTask = it.project.tasks[REPORT_NAME]
                if (dependencyProjectCompileTask != null) {
                    compileTask.dependsOn(dependencyProjectCompileTask)
                    // we don't want this project's tests to affect the other project's report
                    testTasks.each {
                        it.mustRunAfter(dependencyProjectReportTask)
                    }
                }
            }

            compileTask.configure {
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
                scalaCompileOptions.additionalParameters = parameters
                // the compile task creates a store of measured statements
                outputs.file(new File(extension.dataDir.get(), 'scoverage.coverage.xml'))
            }

            project.gradle.taskGraph.whenReady { graph ->
                def hasAnyReportTask = reportTasks.any { graph.hasTask(it) }

                if (hasAnyReportTask) {
                    project.tasks.withType(Test).each { testTask ->
                        testTask.configure {
                            project.logger.info("Adding instrumented classes to '${path}' classpath")

                            classpath = project.configurations.scoverage + instrumentedSourceSet.output + classpath

                            outputs.upToDateWhen {
                                extension.dataDir.get().listFiles(new FilenameFilter() {
                                    @Override
                                    boolean accept(File dir, String name) {
                                        name.startsWith("scoverage.measurements.")
                                    }
                                })
                            }
                        }
                    }

                    compileTask.configure {
                        if (!graph.hasTask(originalCompileTask)) {
                            destinationDir = originalCompileTask.destinationDir
                        } else {
                            doFirst {
                                destinationDir.deleteDir()
                            }

                            // delete non-instrumented classes by comparing normally compiled classes to those compiled with scoverage
                            doLast {
                                def originalCompileTaskName = project.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                                        .getCompileTaskName("scala")
                                def originalDestinationDir = project.tasks[originalCompileTaskName].destinationDir

                                def findFiles = { File dir, Closure<Boolean> condition = null ->
                                    def files = []

                                    if (dir.exists()) {
                                        dir.eachFileRecurse(FILES) { f ->
                                            if (condition == null || condition(f)) {
                                                def relativePath = dir.relativePath(f)
                                                files << relativePath
                                            }
                                        }
                                    }

                                    files
                                }

                                def isSameFile = { String relativePath ->
                                    def fileA = new File(originalDestinationDir, relativePath)
                                    def fileB = new File(destinationDir, relativePath)
                                    FileUtils.contentEquals(fileA, fileB)
                                }

                                def originalClasses = findFiles(originalDestinationDir)
                                def identicalInstrumentedClasses = findFiles(destinationDir, { f ->
                                    def relativePath = destinationDir.relativePath(f)
                                    originalClasses.contains(relativePath) && isSameFile(relativePath)
                                })

                                identicalInstrumentedClasses.each { f ->
                                    Files.deleteIfExists(destinationDir.toPath().resolve(f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Set<? extends Task> recursiveDependenciesOf(Task task) {

        def directDependencies = task.getTaskDependencies().getDependencies(task)
        def nestedDependencies = directDependencies.collect { recursiveDependenciesOf(it) }.flatten()
        directDependencies + nestedDependencies
    }
}