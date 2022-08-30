package org.scoverage

import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.PluginAware
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

import static groovy.io.FileType.FILES

class ScoveragePlugin implements Plugin<PluginAware> {

    static final String CONFIGURATION_NAME = 'scoverage'
    static final String REPORT_NAME = 'reportScoverage'
    static final String CHECK_NAME = 'checkScoverage'
    static final String COMPILE_NAME = 'compileScoverageScala'
    static final String AGGREGATE_NAME = 'aggregateScoverage'
    static final String DEFAULT_SCALA_VERSION = '2.13.6'
    static final String SCOVERAGE_COMPILE_ONLY_PROPERTY = 'scoverageCompileOnly';

    static final String DEFAULT_REPORT_DIR = 'reports' + File.separatorChar + 'scoverage'

    private final WorkerExecutor workerExecutor
    private final ConcurrentHashMap<Task, Set<? extends Task>> crossProjectTaskDependencies = new ConcurrentHashMap<>()
    private final ConcurrentHashMap<Task, Set<? extends Task>> sameProjectTaskDependencies = new ConcurrentHashMap<>()

    @Inject
    ScoveragePlugin(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

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
                def scalaFullVersion = resolveScalaVersion(project)
                def scalaBinaryVersion = scalaFullVersion.substring(0, scalaFullVersion.lastIndexOf('.'))
                def scoverageVersion = project.extensions.scoverage.scoverageVersion.get()

                project.logger.info("Using scoverage scalac plugin $scoverageVersion for scala $scalaFullVersion")

                project.dependencies {
                    scoverage("org.scoverage:scalac-scoverage-plugin_$scalaFullVersion:$scoverageVersion")
                    scoverage("org.scoverage:scalac-scoverage-runtime_$scalaBinaryVersion:$scoverageVersion")
                }
            }
        }

        createTasks(project, extension)
    }

    private void createTasks(Project project, ScoverageExtension extension) {

        ScoverageRunner scoverageRunner = new ScoverageRunner(workerExecutor, project.configurations.scoverage)

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

                def reportTaskName = "report${testTask.name.capitalize()}Scoverage"
                def taskReportDir = project.file("${project.buildDir}/reports/scoverage${testTask.name.capitalize()}")

                project.tasks.create(reportTaskName, ScoverageReport) {
                    dependsOn originalJarTask, compileTask, testTask
                    onlyIf { extension.dataDir.get().list() }
                    group = 'verification'
                    runner = scoverageRunner
                    reportDir = taskReportDir
                    sources.from(originalSourceSet.scala.getSourceDirectories())
                    dataDir = extension.dataDir
                    sourceEncoding.set(detectedSourceEncoding)
                    coverageOutputCobertura = extension.coverageOutputCobertura
                    coverageOutputXML = extension.coverageOutputXML
                    coverageOutputHTML = extension.coverageOutputHTML
                    coverageDebug = extension.coverageDebug
                }
            }

            globalReportTask.configure {
                def dataDirs = reportTasks.findResults { it.dataDir.get() }

                dependsOn reportTasks
                onlyIf { dataDirs.any { it.list() } }

                group = 'verification'
                runner = scoverageRunner
                reportDir = extension.reportDir
                sources.from(originalSourceSet.scala.getSourceDirectories())
                dirsToAggregateFrom = dataDirs
                sourceEncoding.set(detectedSourceEncoding)
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
                outputs.file(new File(extension.dataDir.get(), 'scoverage.coverage'))

                dependsOn project.configurations[CONFIGURATION_NAME]
                doFirst {
                    /*
                        It is crucial that this would run in `doFirst`, as this resolves the (dependencies of the)
                        configuration, which we do not want to do at configuration time (but only at execution time).
                     */
                    def pluginFile = project.configurations[CONFIGURATION_NAME].find {
                        it.name.startsWith("scalac-scoverage-plugin")
                    }
                    scalaCompileOptions.additionalParameters.add('-Xplugin:' + pluginFile.absolutePath)
                }
            }

            if (project.hasProperty(SCOVERAGE_COMPILE_ONLY_PROPERTY)) {
                project.logger.info("Making scoverage compilation the primary compilation task (instead of compileScala)")

                originalCompileTask.enabled = false;
                compileTask.destinationDirectory = originalCompileTask.destinationDirectory

                project.getTasks().each {
                    if (recursiveDependenciesOf(it, true).contains(originalCompileTask)) {
                        it.dependsOn(compileTask)
                    }
                }

                // make this project's scoverage compilation depend on scoverage compilation of any other project
                // which this project depends on its normal compilation
                def originalCompilationDependencies = recursiveDependenciesOf(compileTask, false).findAll {
                    it instanceof ScalaCompile
                }
                originalCompilationDependencies.each {
                    def dependencyProjectCompileTask = it.project.tasks.findByName(COMPILE_NAME)
                    def dependencyProjectReportTask = it.project.tasks.findByName(REPORT_NAME)
                    if (dependencyProjectCompileTask != null) {
                        compileTask.dependsOn(dependencyProjectCompileTask)
                        // we don't want this project's tests to affect the other project's report
                        testTasks.each {
                            it.mustRunAfter(dependencyProjectReportTask)
                        }
                    }
                }
            } else {
                compileTask.configure {
                    doFirst {
                        destinationDir.deleteDir()
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
                        allSources = allSources.plus(it.sources)
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
                        sources.from(allSources)
                        sourceEncoding.set(detectedSourceEncoding)
                        dirsToAggregateFrom = dataDirs
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

    private String resolveScalaVersion(Project project) {

        def scalaVersionProperty = project.extensions.scoverage.scoverageScalaVersion
        if (scalaVersionProperty.isPresent()) {
            def configuredScalaVersion = scalaVersionProperty.get()
            project.logger.info("Using configured Scala version: $configuredScalaVersion")
            return configuredScalaVersion
        } else {
            project.logger.info("No Scala version configured. Detecting scala library...")
            def components = project.configurations.compileClasspath.incoming.resolutionResult.getAllComponents()
            def scalaLibrary = components.find {
                it.moduleVersion.group == "org.scala-lang" && it.moduleVersion.name == "scala-library"
            }
            if (scalaLibrary != null) {
                def scalaVersion = scalaLibrary.moduleVersion.version
                project.logger.info("Detected scala library in compilation classpath. Scala version: $scalaVersion")
                return scalaVersion
            } else {
                project.logger.info("No scala library detected. Using default Scala version: $DEFAULT_SCALA_VERSION")
                return DEFAULT_SCALA_VERSION
            }
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
