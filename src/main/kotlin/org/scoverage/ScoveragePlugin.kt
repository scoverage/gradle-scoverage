package org.scoverage

import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.PluginAware
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.testing.Test

import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the
import java.io.File
import java.math.BigDecimal
import java.nio.file.Path

class ScoveragePlugin : Plugin<PluginAware> {

    companion object {
        const val CONFIGURATION_NAME = "scoverage"
        const val REPORT_NAME = "reportScoverage"
        const val CHECK_NAME = "checkScoverage"
        const val COMPILE_NAME = "compileScoverageScala"
        const val AGGREGATE_NAME = "aggregateScoverage"
        const val DEFAULT_SCALA_VERSION = "2.13.6"
        const val SCOVERAGE_COMPILE_ONLY_PROPERTY = "scoverageCompileOnly"

        @JvmField
        val DEFAULT_REPORT_DIR = ScoverageExtension.DEFAULT_REPORT_DIR
    }

    private val crossProjectTaskDependencies: ConcurrentHashMap<Task, Set<Task>> = ConcurrentHashMap()
    private val sameProjectTaskDependencies: ConcurrentHashMap<Task, Set<Task>> = ConcurrentHashMap()

    override fun apply(pluginAware: PluginAware) {
        if (pluginAware is Project) {
            applyProject(pluginAware)
        } else if (pluginAware is Gradle) {
            pluginAware.allprojects {
                plugins.apply(ScoveragePlugin::class.java)
            }
        } else {
            throw IllegalArgumentException("${pluginAware.javaClass} is currently not supported as an apply target, please report if you need it")
        }
    }

    fun applyProject(project: Project) {

        if (project.plugins.hasPlugin(ScoveragePlugin::class.java)) {
            project.logger.info("Project ${project.name} already has the scoverage plugin")
            return
        }
        project.logger.info("Applying scoverage plugin to $project.name")

        val extension = project.extensions.create("scoverage", ScoverageExtension::class.java, project)
        if (!project.configurations.asMap.containsKey(CONFIGURATION_NAME)) {
            project.configurations.create(CONFIGURATION_NAME) {
                setVisible(false)
                setTransitive(true)
                setDescription("Scoverage dependencies")
            }

            project.afterEvaluate {
                val scalaFullVersion = resolveScalaVersion(project)
                val scalaBinaryVersion = scalaFullVersion.substring(0, scalaFullVersion.lastIndexOf('.'))
                val scoverageVersion =
                    project.extensions.getByType(ScoverageExtension::class.java).scoverageVersion.get()

                project.logger.info("Using scoverage scalac plugin $scoverageVersion for scala $scalaFullVersion")

                project.dependencies.add(
                    CONFIGURATION_NAME,
                    "org.scoverage:scalac-scoverage-plugin_$scalaFullVersion:$scoverageVersion"
                )
                project.dependencies.add(
                    CONFIGURATION_NAME,
                    "org.scoverage:scalac-scoverage-runtime_$scalaBinaryVersion:$scoverageVersion"
                )
            }
        }

        createTasks(project, extension)
    }

    private fun createTasks(project: Project, extension: ScoverageExtension) {

        val scoverageRunner = ScoverageRunner(project.configurations.getByName(CONFIGURATION_NAME))

        val sourceSets = project.the<SourceSetContainer>()
        val originalSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        val originalScalaSourceSet = originalSourceSet.extensions.getByType<SourceDirectorySet>()
        val instrumentedSourceSet = sourceSets.create("scoverage") {
            resources.source(originalSourceSet.resources)
            java.source(originalSourceSet.java)
            extensions.getByType<SourceDirectorySet>().setSrcDirs(originalScalaSourceSet.sourceDirectories)

            annotationProcessorPath += originalSourceSet.annotationProcessorPath + project.configurations.getByName(
                CONFIGURATION_NAME
            )
            compileClasspath += originalSourceSet.compileClasspath + project.configurations.getByName(CONFIGURATION_NAME)
            runtimeClasspath =
                output + project.configurations.getByName(CONFIGURATION_NAME) + originalSourceSet.runtimeClasspath
        }

        val originalCompileTask =
            project.tasks.named(originalSourceSet.getCompileTaskName("scala"), ScalaCompile::class.java).get()
        val originalJarTask = project.tasks.getByName(originalSourceSet.jarTaskName)

        val compileTask =
            project.tasks.named(instrumentedSourceSet.getCompileTaskName("scala"), ScalaCompile::class.java).get()
        compileTask.mustRunAfter(originalCompileTask)

        val globalReportTask = project.tasks.register(REPORT_NAME, ScoverageAggregate::class.java)
        val globalCheckTask = project.tasks.register(CHECK_NAME)

        project.afterEvaluate {
            val detectedSourceEncoding = compileTask.scalaCompileOptions.encoding ?: "UTF-8"

            // calling toList() on TaskCollection is required
            // to avoid potential ConcurrentModificationException in multi-project builds
            val testTasks = project.tasks.withType(Test::class.java).toList()

            val reportTasks: List<ScoverageReport> = testTasks.map { testTask ->
                testTask.mustRunAfter(compileTask)

                val reportTaskName = "report${testTask.name.capitalize()}Scoverage"
                val taskReportDir = project.file("${project.buildDir}/reports/scoverage${testTask.name.capitalize()}")

                project.tasks.create(reportTaskName, ScoverageReport::class.java) {
                    dependsOn(originalJarTask, compileTask, testTask)
                    onlyIf { extension.dataDir.get().list()?.isNotEmpty() ?: false }
                    group = "verification"
                    runner = scoverageRunner
                    reportDir.set(taskReportDir)
                    sources.set(originalScalaSourceSet.sourceDirectories)
                    dataDir.set(extension.dataDir)
                    sourceEncoding.set(detectedSourceEncoding)
                    coverageOutputCobertura.set(extension.coverageOutputCobertura)
                    coverageOutputXML.set(extension.coverageOutputXML)
                    coverageOutputHTML.set(extension.coverageOutputHTML)
                    coverageDebug.set(extension.coverageDebug)
                }
            }

            globalReportTask.configure {
                val dataDirs = reportTasks.map { it.dataDir.get() }

                dependsOn(reportTasks).onlyIf { dataDirs.any { it.list()?.isNotEmpty() ?: false } }

                group = "verification"
                runner = scoverageRunner
                reportDir.set(extension.reportDir)
                sources.from(originalScalaSourceSet.sourceDirectories)
                dirsToAggregateFrom.set(dataDirs)
                sourceEncoding.set(detectedSourceEncoding)
                deleteReportsOnAggregation.set(false)
                coverageOutputCobertura.set(extension.coverageOutputCobertura)
                coverageOutputXML.set(extension.coverageOutputXML)
                coverageOutputHTML.set(extension.coverageOutputHTML)
                coverageDebug.set(extension.coverageDebug)
            }

            configureCheckTask(project, extension, globalCheckTask, globalReportTask)


            compileTask.configure(closureOf<ScalaCompile> {
                val parameters = this.scalaCompileOptions.additionalParameters?.toMutableList() ?: mutableListOf()

                parameters.add("-P:scoverage:dataDir:${extension.dataDir.get().absolutePath}")
                if (extension.excludedPackages.get().isNotEmpty()) {
                    val packages = extension.excludedPackages.get().joinToString(";")
                    parameters.add("-P:scoverage:excludedPackages:$packages")
                }
                if (extension.excludedFiles.get().isNotEmpty()) {
                    val packages = extension.excludedFiles.get().joinToString(";")
                    parameters.add("-P:scoverage:excludedFiles:$packages")
                }
                if (extension.highlighting.get()) {
                    parameters.add("-Yrangepos")
                }
                scalaCompileOptions.additionalParameters = parameters
                // the compile task creates a store of measured statements
                outputs.file(File(extension.dataDir.get(), "scoverage.coverage"))

                dependsOn(project.configurations.getByName(CONFIGURATION_NAME))
                doFirst {
                    /*
                        It is crucial that this would run in `doFirst`, as this resolves the (dependencies of the)
                        configuration, which we do not want to do at configuration time (but only at execution time).
                     */
                    val pluginFile = project.configurations.getByName(CONFIGURATION_NAME).find {
                        it.name.startsWith("scalac-scoverage-plugin")
                    }!!
                    parameters.add("-Xplugin:" + pluginFile.absolutePath)
                }
            })

            if (project.hasProperty(SCOVERAGE_COMPILE_ONLY_PROPERTY)) {
                project.logger.info("Making scoverage compilation the primary compilation task (instead of compileScala)")

                originalCompileTask.enabled = false;
                compileTask.destinationDirectory.set(originalCompileTask.destinationDirectory)

                project.tasks.forEach {
                    if (recursiveDependenciesOf(it, true).contains(originalCompileTask)) {
                        it.dependsOn(compileTask)
                    }
                }

                // make this project's scoverage compilation depend on scoverage compilation of any other project
                // which this project depends on its normal compilation
                val originalCompilationDependencies =
                    recursiveDependenciesOf(compileTask, false).filterIsInstance<ScalaCompile>()
                originalCompilationDependencies.forEach {
                    val dependencyProjectCompileTask = it.project.tasks.findByName(COMPILE_NAME)
                    val dependencyProjectReportTask = it.project.tasks.findByName(REPORT_NAME)
                    if (dependencyProjectCompileTask != null) {
                        compileTask.dependsOn(dependencyProjectCompileTask)
                        // we don't want this project's tests to affect the other project's report
                        testTasks.forEach {
                            it.mustRunAfter(dependencyProjectReportTask)
                        }
                    }
                }
            } else {
                compileTask.configure(closureOf<ScalaCompile> {
                    doFirst {
                        destinationDirectory.get().asFile.deleteRecursively()
                    }

                    // delete non-instrumented classes by comparing normally compiled classes to those compiled with scoverage
                    doLast {
                        project.logger.info("Deleting classes compiled by scoverage but non-instrumented (identical to normal compilation)")
                        val originalDestinationDir = originalCompileTask.destinationDirectory.get().asFile.toPath()
                        val destinationDir = destinationDirectory.get().asFile.toPath()

                        fun findFiles(dir: Path, condition: (Path) -> Boolean = { _ -> true }): Sequence<Path> {
                            return dir.toFile().walk().filter { f ->
                                f.isFile && condition(f.toPath())
                            }.map { f ->
                                dir.relativize(f.toPath())
                            }
                        }

                        fun isSameFile(relativePath: Path): Boolean {
                            val fileA = originalDestinationDir.resolve(relativePath)
                            val fileB = destinationDir.resolve(relativePath)
                            return FileUtils.contentEquals(fileA.toFile(), fileB.toFile())
                        }

                        val originalClasses = findFiles(originalDestinationDir)
                        val identicalInstrumentedClasses = findFiles(destinationDir) {
                            val relativePath = destinationDir.relativize(it)
                            originalClasses.contains(relativePath) && isSameFile(relativePath)
                        }

                        identicalInstrumentedClasses.forEach {
                            Files.deleteIfExists(destinationDir.resolve(it))
                        }
                    }
                })
            }

            project.gradle.taskGraph.whenReady(closureOf<TaskExecutionGraph> {
                val hasAnyReportTask = reportTasks.any { this.hasTask(it) }

                if (hasAnyReportTask) {
                    project.tasks.withType(Test::class.java).forEach {
                        it.configure(closureOf<Test> {
                            project.logger.info("Adding instrumented classes to '${path}' classpath")

                            classpath =
                                project.configurations.getByName(CONFIGURATION_NAME) + instrumentedSourceSet.output + classpath

                            outputs.upToDateWhen {
                                extension.dataDir.get().listFiles { _, name ->
                                    name.startsWith("scoverage.measurements.")
                                }?.isNotEmpty() ?: false
                            }
                        })
                    }
                }
            })

            // define aggregation task
            if (project.subprojects.isNotEmpty()) {
                project.gradle.projectsEvaluated {
                    project.subprojects.forEach {
                        if (it.plugins.hasPlugin(ScalaPlugin::class.java) && !it.plugins.hasPlugin(ScoveragePlugin::class.java)) {
                            it.logger.warn("Scala sub-project '${it.name}' doesn't have Scoverage applied and will be ignored in parent project aggregation")
                        }
                    }
                    val childReportTasks = project.subprojects.flatMap {
                        it.tasks.filterIsInstance<ScoverageAggregate>().filter { task ->
                            task.name == REPORT_NAME
                        }
                    }
                    val allReportTasks = childReportTasks + globalReportTask.get()
                    val allSources = project.objects.fileCollection()
                    allReportTasks.forEach {
                        allSources.from(it.sources)
                    }
                    val aggregationTask = project.tasks.create(AGGREGATE_NAME, ScoverageAggregate::class.java) {
                        val dataDirs = allReportTasks.map { it.dirsToAggregateFrom.get() }.flatten()
                        onlyIf {
                            childReportTasks.isNotEmpty()
                        }
                        dependsOn(allReportTasks)
                        group = "verification"
                        runner = scoverageRunner
                        reportDir.set(extension.reportDir)
                        sources.from(allSources)
                        sourceEncoding.set(detectedSourceEncoding)
                        dirsToAggregateFrom.set(dataDirs)
                        deleteReportsOnAggregation.set(extension.deleteReportsOnAggregation)
                        coverageOutputCobertura.set(extension.coverageOutputCobertura)
                        coverageOutputXML.set(extension.coverageOutputXML)
                        coverageOutputHTML.set(extension.coverageOutputHTML)
                        coverageDebug.set(extension.coverageDebug)
                    }
                    project.tasks.getByName(CHECK_NAME).mustRunAfter(aggregationTask)
                }
            }
        }
    }

    private fun configureCheckTask(
        project: Project, extension: ScoverageExtension,
        globalCheckTask: TaskProvider<Task>,
        globalReportTask: TaskProvider<ScoverageAggregate>
    ) {

        if (extension.checks.isEmpty()) {
            extension.check(closureOf<ScoverageExtension.CheckConfig> {
                minimumRate =
                    extension.minimumRate.getOrElse(BigDecimal.valueOf(ScoverageExtension.DEFAULT_MINIMUM_RATE))
                coverageType = extension.coverageType.getOrElse(ScoverageExtension.DEFAULT_COVERAGE_TYPE)
            })
        } else if (extension.minimumRate.isPresent() || extension.coverageType.isPresent()) {
            throw IllegalArgumentException("Check configuration should be defined in either the new or the old syntax exclusively, not together")
        }

        val checker = CoverageChecker(project.logger)

        globalCheckTask.configure {
            group = "verification"
            dependsOn(globalReportTask).onlyIf { extension.reportDir.get().list()?.isNotEmpty() ?: false }
        }

        extension.checks.forEach { config ->
            globalCheckTask.configure {
                doLast {
                    checker.checkLineCoverage(
                        extension.reportDir.get(),
                        config.coverageType!!,
                        config.minimumRate?.toDouble()!!
                    )
                }
            }
        }
    }

    private fun resolveScalaVersion(project: Project): String {

        val scalaVersionProperty = project.extensions.getByType(ScoverageExtension::class.java).scoverageScalaVersion
        if (scalaVersionProperty.isPresent) {
            val configuredScalaVersion = scalaVersionProperty.get()
            project.logger.info("Using configured Scala version: $configuredScalaVersion")
            return configuredScalaVersion
        } else {
            project.logger.info("No Scala version configured. Detecting scala library...")
            val components =
                project.configurations.getByName("compileClasspath").incoming.resolutionResult.allComponents
            val scalaLibrary = components.find {
                it.moduleVersion?.group == "org.scala-lang" && it.moduleVersion?.name == "scala-library"
            }
            if (scalaLibrary != null) {
                val scalaVersion = scalaLibrary.moduleVersion?.version!!
                project.logger.info("Detected scala library in compilation classpath. Scala version: $scalaVersion")
                return scalaVersion
            } else {
                project.logger.info("No scala library detected. Using default Scala version: $DEFAULT_SCALA_VERSION")
                return DEFAULT_SCALA_VERSION
            }
        }
    }

    private fun recursiveDependenciesOf(task: Task, sameProjectOnly: Boolean): Set<Task> {
        val cache = if (sameProjectOnly) sameProjectTaskDependencies else crossProjectTaskDependencies

        return cache[task] ?: run {
            var directDependencies: Set<Task> = task.taskDependencies.getDependencies(task)
            if (sameProjectOnly) {
                directDependencies = directDependencies.filter {
                    it.project == task.project
                }.toSet()
            }
            val nestedDependencies = directDependencies.flatMap { recursiveDependenciesOf(it, sameProjectOnly) }
            val dependencies = directDependencies + nestedDependencies

            cache.put(task, dependencies)
            dependencies
        }
    }
}
