package org.scoverage

import org.apache.commons.io.FileUtils
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.PluginAware
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.util.PatternFilterable

import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

import static groovy.io.FileType.FILES

class ScoveragePlugin implements Plugin<PluginAware> {

    static final String CONFIGURATION_NAME = 'scoverage'
    static final String MERGE_MEASUREMENTS_NAME = 'mergeScoverageMeasurements'
    static final String REPORT_NAME = 'reportScoverage'
    static final String CHECK_NAME = 'checkScoverage'
    static final String COMPILE_NAME = 'compileScoverageScala'
    static final String AGGREGATE_NAME = 'aggregateScoverage'
    static final String DEFAULT_SCALA_VERSION = '2.13.6'
    static final String SCOVERAGE_COMPILE_ONLY_PROPERTY = 'scoverageCompileOnly';

    static final String DEFAULT_REPORT_DIR = 'reports' + File.separatorChar + 'scoverage'

    private final ConcurrentHashMap<Task, Set<? extends Task>> crossProjectTaskDependencies = new ConcurrentHashMap<>()
    private final ConcurrentHashMap<Task, Set<? extends Task>> sameProjectTaskDependencies = new ConcurrentHashMap<>()

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
                def scalaVersion = resolveScalaVersions(project)

                def scoverageVersion = project.extensions.scoverage.scoverageVersion.get()
                project.logger.info("Using scoverage scalac plugin $scoverageVersion for scala $scalaVersion")

                def scalacScoverageVersion = scalaVersion.scalacScoverageVersion
                def scalacScoveragePluginVersion = scalaVersion.scalacScoveragePluginVersion
                def scalacScoverageRuntimeVersion = scalaVersion.scalacScoverageRuntimeVersion

                project.dependencies {
                    scoverage("org.scoverage:scalac-scoverage-domain_$scalacScoverageVersion:$scoverageVersion")
                    scoverage("org.scoverage:scalac-scoverage-reporter_$scalacScoverageVersion:$scoverageVersion")
                    scoverage("org.scoverage:scalac-scoverage-serializer_$scalacScoverageVersion:$scoverageVersion")
                    scoverage("org.scoverage:scalac-scoverage-runtime_$scalacScoverageRuntimeVersion:$scoverageVersion")
                    scoverage("org.scoverage:scalac-scoverage-plugin_$scalacScoveragePluginVersion:$scoverageVersion")
                }
            }
        }

        createTasks(project, extension)
    }

    private void createTasks(Project project, ScoverageExtension extension) {
        /**
         dataDir is split into subdirectories:
           workDir
           {testTaskName}MeasurementsDir
           {testTaskName}ReportDir.

         workDir
         Directory where metadata/measurements are "produced"
         Two tasks produce files in that directory: compile and test. Because of that, this directory is not
         cacheable.
         Only one file from this directory is cached: metadata from compile task (because it is a single file).

         testMeasurementsDir
         Directory where measurements are synced from "workDir". It is registered as an additional output to test task,
         which makes the measurements files cacheable.

         reportDir
         Merges workDir/scoverage.coverage and testMeasurementsDir/scoverage.measurements.* for reporting.
         */
        def dataWorkDir = extension.dataDir.map {new File(it, "work") }

        ScoverageRunner scoverageRunner = new ScoverageRunner(project.configurations.scoverage)

        def originalSourceSet = project.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        def instrumentedSourceSet = project.sourceSets.create('scoverage') {

            resources.source(originalSourceSet.resources)
            java.source(originalSourceSet.java)
            scala.source(originalSourceSet.scala)

            annotationProcessorPath += originalSourceSet.annotationProcessorPath + project.configurations.scoverage
            compileClasspath += originalSourceSet.compileClasspath + project.configurations.scoverage
            runtimeClasspath = it.output + project.configurations.scoverage + originalSourceSet.runtimeClasspath
        }

        def originalCompileTask = project.tasks[originalSourceSet.getCompileTaskName("scala")]
        def originalJarTask = project.tasks[originalSourceSet.getJarTaskName()]

        def compileTask = project.tasks[instrumentedSourceSet.getCompileTaskName("scala")]
        compileTask.mustRunAfter(originalCompileTask)

        // merges measurements from individual reports into one directory for use by globalReportTask
        def globalMergeMeasurementsTask = project.tasks.register(MERGE_MEASUREMENTS_NAME, Sync.class)

        def globalReportTask = project.tasks.register(REPORT_NAME, ScoverageAggregate)
        def globalCheckTask = project.tasks.register(CHECK_NAME)

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

                def cTaskName = testTask.name.capitalize()

                def reportTaskName = "report${cTaskName}Scoverage"
                def taskReportDir = project.file("${project.buildDir}/reports/scoverage${cTaskName}")

                def scoverageSyncMetaWithOutputs =
                        project.tasks.register("sync${cTaskName}ScoverageData", Sync.class)

                scoverageSyncMetaWithOutputs.configure {
                    dependsOn compileTask, testTask
                    from(dataWorkDir) {
                        include("scoverage.coverage")
                    }
                    from(dataMeasurementsDir(extension, cTaskName))
                    into(dataReportDir(extension, cTaskName))
                }

                project.tasks.create(reportTaskName, ScoverageReport) {
                    dependsOn originalJarTask, compileTask, testTask, scoverageSyncMetaWithOutputs
                    onlyIf { scoverageSyncMetaWithOutputs.get().getDestinationDir().list() }
                    group = 'verification'
                    runner = scoverageRunner
                    reportDir = taskReportDir
                    sources = originalSourceSet.scala.getSourceDirectories()
                    dataDir = scoverageSyncMetaWithOutputs.map {it.getDestinationDir()}
                    sourceEncoding.set(detectedSourceEncoding)
                    coverageOutputCobertura = extension.coverageOutputCobertura
                    coverageOutputXML = extension.coverageOutputXML
                    coverageOutputHTML = extension.coverageOutputHTML
                    coverageDebug = extension.coverageDebug
                }
            }

            globalMergeMeasurementsTask.configure {sync ->
                dependsOn(reportTasks)
                dependsOn(compileTask)

                def dataDirs = reportTasks.findResults { it.dataDir.get() }

                from(dataWorkDir.map {new File(it, 'scoverage.coverage') })
                from(dataDirs) {
                    exclude("scoverage.coverage")
                }
                into(project.file("${project.buildDir}/mergedScoverage"))
            }

            globalReportTask.configure {
                dependsOn globalMergeMeasurementsTask

                onlyIf { globalMergeMeasurementsTask.get().getDestinationDir().list()}

                group = 'verification'
                runner = scoverageRunner
                reportDir = extension.reportDir
                sources = originalSourceSet.scala.getSourceDirectories()
                dirsToAggregateFrom = globalMergeMeasurementsTask.map {[it.getDestinationDir()]}
                sourceEncoding.set(detectedSourceEncoding)
                deleteReportsOnAggregation = false
                coverageOutputCobertura = extension.coverageOutputCobertura
                coverageOutputXML = extension.coverageOutputXML
                coverageOutputHTML = extension.coverageOutputHTML
                coverageDebug = extension.coverageDebug
            }

            configureCheckTask(project, extension, globalCheckTask, globalReportTask)

            compileTask.configure {
                List<String> parameters = []
                List<String> existingParameters = scalaCompileOptions.additionalParameters
                if (existingParameters) {
                    parameters.addAll(existingParameters)
                }

                def scalaVersion = resolveScalaVersions(project)

                // the compile task creates a store of measured statements
                outputs.file(dataWorkDir.map {new File(it, 'scoverage.coverage') })

                if (scalaVersion.majorVersion < 3) {
                    parameters.add("-P:scoverage:dataDir:${dataWorkDir.get().absolutePath}".toString())
                    parameters.add("-P:scoverage:sourceRoot:${extension.project.getRootDir().absolutePath}".toString())
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
                    dependsOn project.configurations.named(CONFIGURATION_NAME)
                    doFirst {
                        /*
                            It is crucial that this would run in `doFirst`, as this resolves the (dependencies of the)
                            configuration, which we do not want to do at configuration time (but only at execution time).
                         */
                        def pluginFiles = project.configurations[CONFIGURATION_NAME].findAll {
                            it.name.startsWith("scalac-scoverage-plugin") ||
                            it.name.startsWith("scalac-scoverage-domain") ||
                            it.name.startsWith("scalac-scoverage-serializer")
                        }.collect {
                            it.absolutePath
                        }
                        scalaCompileOptions.additionalParameters.add('-Xplugin:' + pluginFiles.join(File.pathSeparator))
                    }
                } else {
                    parameters.add("-sourceroot:${project.rootDir.absolutePath}".toString())
                    parameters.add("-coverage-out:${dataWorkDir.get().absolutePath}".toString())
                    scalaCompileOptions.additionalParameters = parameters
                }
            }

            compileTask.configure {
                doFirst {
                    destinationDirectory.get().getAsFile().deleteDir()
                }

                // delete non-instrumented classes by comparing normally compiled classes to those compiled with scoverage
                doLast {
                    project.logger.info("Deleting classes compiled by scoverage but non-instrumented (identical to normal compilation)")
                    def originalCompileTaskName = project.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                            .getCompileTaskName("scala")
                    def originalDestinationDirectory = project.tasks[originalCompileTaskName].destinationDirectory
                    def originalDestinationDir = originalDestinationDirectory.get().asFile
                    def destinationDir = destinationDirectory.get().asFile


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

            project.gradle.taskGraph.whenReady { graph ->
                def hasAnyReportTask = reportTasks.any { graph.hasTask(it) }

                if (hasAnyReportTask) {

                    project.tasks.withType(Test).each { testTask ->
                        testTask.configure {
                            def cTaskName = testTask.name.capitalize()

                            project.logger.info("Adding instrumented classes to '${path}' classpath")

                            classpath = project.configurations.scoverage + instrumentedSourceSet.output + classpath

                            doLast {
                                project.sync {
                                    from(dataWorkDir)
                                    exclude("scoverage.coverage")
                                    into(dataMeasurementsDir(extension, cTaskName))
                                }
                                project.delete(project.fileTree(dataWorkDir).exclude("scoverage.coverage"))
                            }

                            outputs.dir(dataMeasurementsDir(extension, cTaskName)).withPropertyName("scoverage.measurements")

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
                    def allReportTasks = childReportTasks + globalReportTask.get()
                    def allSources = project.objects.fileCollection()
                    allReportTasks.each {
                        allSources = allSources.plus(it.sources.get())
                    }
                    def aggregationTask = project.tasks.create(AGGREGATE_NAME, ScoverageAggregate) {
                        def dataDirs = allReportTasks.findResults { it.dirsToAggregateFrom.get() }.flatten()
                        onlyIf {
                            !childReportTasks.empty
                        }
                        dependsOn(allReportTasks)
                        group = 'verification'
                        runner = scoverageRunner
                        reportDir = extension.reportDir
                        sources = allSources
                        sourceEncoding.set(detectedSourceEncoding)
                        dirsToAggregateFrom = dataDirs
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

    private Provider<File> dataMeasurementsDir(ScoverageExtension extension, String testName) {
        return extension.dataDir.map {new File(it, "${testName}Measurements") }
    }

    private Provider<File> dataReportDir(ScoverageExtension extension, String testName) {
        return extension.dataDir.map { new File(it, "${testName}Report") }
    }

    private void configureCheckTask(Project project, ScoverageExtension extension,
                                    TaskProvider<Task> globalCheckTask,
                                    TaskProvider<ScoverageAggregate> globalReportTask) {

        if (extension.checks.isEmpty()) {
            extension.check {
                minimumRate = extension.minimumRate.getOrElse(BigDecimal.valueOf(ScoverageExtension.DEFAULT_MINIMUM_RATE))
                coverageType = extension.coverageType.getOrElse(ScoverageExtension.DEFAULT_COVERAGE_TYPE)
            }
        } else if (extension.minimumRate.isPresent() || extension.coverageType.isPresent()) {
            throw new IllegalArgumentException("Check configuration should be defined in either the new or the old syntax exclusively, not together")
        }

        def checker = new CoverageChecker(project.logger)

        globalCheckTask.configure {
            group = 'verification'
            dependsOn globalReportTask
            onlyIf { extension.reportDir.get().list() }
        }

        extension.checks.each { config ->
            globalCheckTask.configure {
                doLast {
                    checker.checkLineCoverage(extension.reportDir.get(), config.coverageType, config.minimumRate.doubleValue())
                }
            }
        }
    }

    private ScalaVersion resolveScalaVersions(Project project) {
        def scalaVersionProperty = project.extensions.scoverage.scoverageScalaVersion
        if (scalaVersionProperty.isPresent()) {
            def configuredScalaVersion = scalaVersionProperty.get()
            project.logger.info("Using configured Scala version: $configuredScalaVersion")
            return new ScalaVersion(configuredScalaVersion)
        } else {
            project.logger.info("No Scala version configured. Detecting scala library...")
            def components = project.configurations.compileClasspath.incoming.resolutionResult.getAllComponents()

            def scala3Library = components.find {
                it.moduleVersion.group == "org.scala-lang" && it.moduleVersion.name == "scala3-library_3"
            }
            def scalaLibrary = components.find {
                it.moduleVersion.group == "org.scala-lang" && it.moduleVersion.name == "scala-library"
            }

            // Scala 3
            if (scala3Library != null) {
                def scala3Version = scala3Library.moduleVersion.version
                def scala2Version = scalaLibrary.moduleVersion.version
                project.logger.info("Detected scala 3 library in compilation classpath. Scala 3 version: $scala3Version; using Scala 2 library: $scala2Version")
                return new ScalaVersion(scala3Version, Optional.of(scala2Version))
            }

            // Scala 2
            if (scalaLibrary != null) {
                def scala2Version = scalaLibrary.moduleVersion.version
                project.logger.info("Detected scala library in compilation classpath. Scala version: $scala2Version")
                return new ScalaVersion(scala2Version)
            }

            // No Scala library was found, using default Scala version
            project.logger.info("No scala library detected. Using default Scala version: $DEFAULT_SCALA_VERSION")
            return new ScalaVersion(DEFAULT_SCALA_VERSION)
        }
    }

    private Set<? extends Task> recursiveDependenciesOf(Task task, boolean sameProjectOnly) {
        def cache = sameProjectOnly ? sameProjectTaskDependencies : crossProjectTaskDependencies
        if (!cache.containsKey(task)) {
            def directDependencies = task.getTaskDependencies().getDependencies(task)
            if (sameProjectOnly) {
                directDependencies = directDependencies.findAll {
                    it.project == task.project
                }
            }
            def nestedDependencies = directDependencies.collect { recursiveDependenciesOf(it, sameProjectOnly) }.flatten()
            def dependencies = directDependencies + nestedDependencies

            cache.put(task, dependencies)
            return dependencies
        } else {
            return cache.get(task)
        }
    }
}
