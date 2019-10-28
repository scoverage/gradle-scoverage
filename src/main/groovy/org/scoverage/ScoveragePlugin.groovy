package org.scoverage

import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.PluginAware
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.testing.Test

import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

import static groovy.io.FileType.FILES

class ScoveragePlugin implements Plugin<PluginAware> {

    static final String CONFIGURATION_NAME = 'scoverage'
    static final String REPORT_NAME = 'reportScoverage'
    static final String CHECK_NAME = 'checkScoverage'
    static final String COMPILE_NAME = 'compileScoverageScala'
    static final String AGGREGATE_NAME = 'aggregateScoverage'

    static final String DEFAULT_REPORT_DIR = 'reports' + File.separatorChar + 'scoverage'

    private volatile File pluginFile = null
    private final ConcurrentHashMap<Task, Set<? extends Task>> taskDependencies = new ConcurrentHashMap<>();

    @Override
    void apply(PluginAware pluginAware) {
        if (pluginAware instanceof Project) {
            applyProject(pluginAware)
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
                def scalaVersion = resolveScalaVersion(project)
                def scoverageVersion = project.extensions.scoverage.scoverageVersion.get()
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
            java.source(originalSourceSet.java)
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
            def detectedSourceEncoding = compileTask.scalaCompileOptions.encoding
            if (detectedSourceEncoding == null) {
                detectedSourceEncoding = "UTF-8"
            }

            // calling toList() on TaskCollection is required
            // to avoid potential ConcurrentModificationException in multi-project builds
            def testTasks = project.tasks.withType(Test).toList()

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
                    sourceEncoding.set(detectedSourceEncoding)
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
                sourceEncoding.set(detectedSourceEncoding)
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
                if (pluginFile == null) {
                    pluginFile = project.configurations[CONFIGURATION_NAME].find {
                        it.name.startsWith("scalac-scoverage-plugin")
                    }
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
                }

                compileTask.configure {
                    if (!graph.hasTask(originalCompileTask)) {
                        project.logger.info("Making scoverage compilation the primary compilation task (instead of compileScala)")
                        destinationDir = originalCompileTask.destinationDir
                    } else {
                        doFirst {
                            destinationDir.deleteDir()
                        }

                        // delete non-instrumented classes by comparing normally compiled classes to those compiled with scoverage
                        doLast {
                            project.logger.info("Deleting classes compiled by scoverage but non-instrumented (identical to normal compilation)")
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

            // define aggregation task
            if (!project.subprojects.empty) {
                project.gradle.projectsEvaluated {
                    project.subprojects.each {
                        if (it.plugins.hasPlugin(ScalaPlugin) && !it.plugins.hasPlugin(ScoveragePlugin)) {
                            it.logger.warn("Scala sub-project '${it.name}' doesn't have Scoverage applied and will be ignored in parent project aggregation")
                        }
                    }
                    def childReportTasks = project.subprojects.findResults {
                        it.tasks.find { task ->
                            task.name == REPORT_NAME && task instanceof ScoverageAggregate
                        }
                    }
                    def allReportTasks = childReportTasks + globalReportTask
                    def aggregationTask = project.tasks.create(AGGREGATE_NAME, ScoverageAggregate) {
                        onlyIf {
                            !childReportTasks.empty
                        }
                        dependsOn(allReportTasks)
                        group = 'verification'
                        runner = scoverageRunner
                        reportDir = extension.reportDir
                        sourceEncoding.set(detectedSourceEncoding)
                        deleteReportsOnAggregation = extension.deleteReportsOnAggregation
                        coverageOutputCobertura = extension.coverageOutputCobertura
                        coverageOutputXML = extension.coverageOutputXML
                        coverageOutputHTML = extension.coverageOutputHTML
                        coverageDebug = extension.coverageDebug
                    }
                    project.tasks[CHECK_NAME].mustRunAfter(aggregationTask)
                }
            }
        }
    }

    private String resolveScalaVersion(Project project) {

        def resolvedDependencies = project.configurations.compileClasspath.resolvedConfiguration.firstLevelModuleDependencies

        def scalaLibrary = resolvedDependencies.find {
            it.moduleGroup == "org.scala-lang" && it.moduleName == "scala-library"
        }

        if (scalaLibrary == null) {
            project.logger.info("No scala library detected. Using property 'scoverageScalaVersion'")
            return project.extensions.scoverage.scoverageScalaVersion.get()
        } else {
            project.logger.info("Detected scala library in compilation classpath")
            def fullScalaVersion = scalaLibrary.moduleVersion
            return fullScalaVersion.substring(0, fullScalaVersion.lastIndexOf("."))
        }
    }

    private Set<? extends Task> recursiveDependenciesOf(Task task) {
        if (!taskDependencies.containsKey(task)) {
            def directDependencies = task.getTaskDependencies().getDependencies(task)
            def nestedDependencies = directDependencies.collect { recursiveDependenciesOf(it) }.flatten()
            def dependencies = directDependencies + nestedDependencies

            taskDependencies.put(task, dependencies)
            return dependencies
        } else {
            return taskDependencies.get(task)
        }
    }
}
