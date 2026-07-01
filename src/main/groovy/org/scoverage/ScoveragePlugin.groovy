package org.scoverage

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.PluginAware
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test

import java.util.Optional

class ScoveragePlugin implements Plugin<PluginAware> {

    static final String CONFIGURATION_NAME = 'scoverage'
    static final String REPORT_NAME = 'reportScoverage'
    static final String CHECK_NAME = 'checkScoverage'
    static final String COMPILE_NAME = 'compileScoverageScala'
    static final String AGGREGATE_NAME = 'aggregateScoverage'
    static final String DEFAULT_SCALA_VERSION = '2.13.14'
    static final String SCOVERAGE_COMPILE_ONLY_PROPERTY = 'scoverageCompileOnly';

    static final String DEFAULT_REPORT_DIR = 'reports' + File.separatorChar + 'scoverage'

    static String scoverageTestTaskName(String testTaskName) {
        return ScoverageTestSupport.scoverageTestTaskName(testTaskName)
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
                canBeResolved = true
                canBeConsumed = false
            }

            project.afterEvaluate {
                configureScoverageDependencies(project)
            }
        }

        createTasks(project, extension)
    }

    private void configureScoverageDependencies(Project project) {
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

    private void createTasks(Project project, ScoverageExtension extension) {

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

        def globalReportTask = project.tasks.register(REPORT_NAME, ScoverageAggregate)
        def globalCheckTask = project.tasks.register(CHECK_NAME, CheckScoverageTask)

        project.afterEvaluate {
            def scoverageDataDir = extension.dataDir
            def scoverageReportDir = extension.reportDir
            def projectRootDir = project.rootDir
            def hasScoverageSubproject = project.objects.property(Boolean)
            hasScoverageSubproject.set(false)

            def detectedSourceEncoding = compileTask.scalaCompileOptions.encoding
            if (detectedSourceEncoding == null) {
                detectedSourceEncoding = "UTF-8"
            }

            def scalaVersion = resolveScalaVersions(project)

            // calling toList() on TaskCollection is required
            // to avoid potential ConcurrentModificationException in multi-project builds
            def testTasks = project.tasks.withType(Test).matching { testTask ->
                !ScoverageTestSupport.isScoverageTestTask(testTask.name)
            }.toList()

            testTasks.each { testTask ->
                ScoverageTestSupport.registerScoverageTest(
                        project,
                        testTask,
                        instrumentedSourceSet,
                        CONFIGURATION_NAME,
                        compileTask,
                        scoverageDataDir
                )
            }

            List<ScoverageReport> reportTasks = testTasks.collect { testTask ->
                testTask.mustRunAfter(compileTask)

                def reportTaskName = "report${testTask.name.capitalize()}Scoverage"
                def taskReportDir = project.layout.buildDirectory.dir("reports/scoverage${testTask.name.capitalize()}").get().asFile
                def scoverageTestTask = project.tasks.named(
                        ScoverageTestSupport.scoverageTestTaskName(testTask.name),
                        Test
                )

                project.tasks.create(reportTaskName, ScoverageReport) {
                    dependsOn originalJarTask, compileTask, scoverageTestTask
                    onlyIf { scoverageDataDir.get().list() }
                    group = 'verification'
                    runner.set(scoverageRunner)
                    reportDir.set(taskReportDir)
                    sources.set(originalSourceSet.scala.getSourceDirectories())
                    dataDir.set(scoverageDataDir.get())
                    sourceEncoding.set(detectedSourceEncoding)
                    sourceRoot.set(projectRootDir)
                    coverageOutputCobertura.set(extension.coverageOutputCobertura.get())
                    coverageOutputXML.set(extension.coverageOutputXML.get())
                    coverageOutputHTML.set(extension.coverageOutputHTML.get())
                    coverageDebug.set(extension.coverageDebug.get())
                }
            }

            globalReportTask.configure {
                def dataDirs = reportTasks.findResults { it.dataDir.get() }

                dependsOn reportTasks
                onlyIf { dataDirs.any { it.list() } }

                group = 'verification'
                runner.set(scoverageRunner)
                reportDir.set(scoverageReportDir.get())
                sources.set(originalSourceSet.scala.getSourceDirectories())
                dirsToAggregateFrom.set(dataDirs)
                sourceEncoding.set(detectedSourceEncoding)
                sourceRoot.set(projectRootDir)
                deleteReportsOnAggregation.set(false)
                coverageOutputCobertura.set(extension.coverageOutputCobertura.get())
                coverageOutputXML.set(extension.coverageOutputXML.get())
                coverageOutputHTML.set(extension.coverageOutputHTML.get())
                coverageDebug.set(extension.coverageDebug.get())
            }

            configureCheckTask(extension, globalCheckTask, globalReportTask, scoverageReportDir)

            compileTask.configure {
                List<String> parameters = []
                List<String> existingParameters = scalaCompileOptions.additionalParameters
                if (existingParameters) {
                    parameters.addAll(existingParameters)
                }

                if (scalaVersion.majorVersion < 3) {
                    parameters.add("-P:scoverage:dataDir:${scoverageDataDir.get().absolutePath}".toString())
                    parameters.add("-P:scoverage:sourceRoot:${projectRootDir.absolutePath}".toString())
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
                    outputs.file(new File(scoverageDataDir.get(), 'scoverage.coverage'))

                    dependsOn project.configurations[CONFIGURATION_NAME]
                    def scoverageClasspath = project.configurations[CONFIGURATION_NAME]
                    doFirst {
                        def pluginFiles = scoverageClasspath.files.findAll { file ->
                            def name = file.name
                            name.startsWith('scalac-scoverage-plugin') ||
                                    name.startsWith('scalac-scoverage-domain') ||
                                    name.startsWith('scalac-scoverage-serializer')
                        }.collect { it.absolutePath }
                        if (!pluginFiles.isEmpty()) {
                            scalaCompileOptions.additionalParameters.add(
                                    '-Xplugin:' + pluginFiles.join(File.pathSeparator)
                            )
                        }
                    }
                } else {
                    parameters.add("-sourceroot:${projectRootDir.absolutePath}".toString())
                    parameters.add("-coverage-out:${scoverageDataDir.get().absolutePath}".toString())
                    if (extension.excludedPackages.get()) {
                        def packages = extension.excludedPackages.get().join(',')
                        parameters.add("-coverage-exclude-classlikes:$packages".toString())
                    }
                    if (extension.excludedFiles.get()) {
                        def packages = extension.excludedFiles.get().join(';')
                        parameters.add("-coverage-exclude-files:$packages".toString())
                    }
                    scalaCompileOptions.additionalParameters = parameters
                }
            }

            def pruneIdenticalScoverageClasses = project.tasks.register('pruneIdenticalScoverageClasses', PruneIdenticalScoverageClassesTask) {
                dependsOn compileTask
                originalClassesDirectory.set(originalCompileTask.destinationDirectory)
                instrumentedClassesDirectory.set(compileTask.destinationDirectory)
            }

            compileTask.configure {
                doFirst {
                    destinationDirectory.get().getAsFile().deleteDir()
                }
                if (scalaVersion.majorVersion < 3) {
                    finalizedBy(pruneIdenticalScoverageClasses)
                }
            }

            if (!project.subprojects.empty) {
                configureRootAggregation(
                        project,
                        extension,
                        scoverageRunner,
                        detectedSourceEncoding,
                        globalReportTask,
                        globalCheckTask,
                        scoverageDataDir,
                        projectRootDir,
                        hasScoverageSubproject
                )
            }
        }
    }

    private void configureRootAggregation(Project project,
                                          ScoverageExtension extension,
                                          ScoverageRunner scoverageRunner,
                                          String detectedSourceEncoding,
                                          TaskProvider<ScoverageAggregate> globalReportTask,
                                          TaskProvider<CheckScoverageTask> globalCheckTask,
                                          org.gradle.api.provider.Property<File> scoverageDataDir,
                                          File projectRootDir,
                                          org.gradle.api.provider.Property<Boolean> hasScoverageSubproject) {
        def aggregatedSources = project.objects.fileCollection()
        if (project.plugins.hasPlugin(ScalaPlugin)) {
            aggregatedSources.from(project.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).scala.sourceDirectories)
        }
        def aggregateTask = project.tasks.register(AGGREGATE_NAME, ScoverageAggregate) {
            dependsOn(globalReportTask)
            group = 'verification'
            runner.set(scoverageRunner)
            reportDir.set(extension.reportDir.get())
            sources.set(aggregatedSources)
            sourceEncoding.set(detectedSourceEncoding)
            sourceRoot.set(projectRootDir)
            deleteReportsOnAggregation.set(extension.deleteReportsOnAggregation.get())
            coverageOutputCobertura.set(extension.coverageOutputCobertura.get())
            coverageOutputXML.set(extension.coverageOutputXML.get())
            coverageOutputHTML.set(extension.coverageOutputHTML.get())
            coverageDebug.set(extension.coverageDebug.get())
            dirsToAggregateFrom.add(scoverageDataDir.get())
            onlyIf { hasScoverageSubproject.get() }
        }

        globalCheckTask.configure {
            mustRunAfter(aggregateTask)
        }

        project.subprojects.each { sub ->
            sub.plugins.withId('scala') {
                if (!sub.plugins.hasPlugin(ScoveragePlugin)) {
                    sub.logger.warn("Scala sub-project '${sub.name}' doesn't have Scoverage applied and will be ignored in parent project aggregation")
                }
            }
            sub.plugins.withId('org.scoverage') {
                hasScoverageSubproject.set(true)
                aggregateTask.configure { aggregationTask ->
                    aggregationTask.dependsOn(sub.tasks.named(REPORT_NAME))
                    aggregatedSources.from(
                            sub.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).scala.sourceDirectories
                    )
                    aggregationTask.dirsToAggregateFrom.add(sub.extensions.scoverage.dataDir.get())
                }
            }
        }
    }

    private void configureCheckTask(ScoverageExtension extension,
                                    TaskProvider<CheckScoverageTask> globalCheckTask,
                                    TaskProvider<ScoverageAggregate> globalReportTask,
                                    org.gradle.api.provider.Property<File> scoverageReportDir) {

        if (extension.checks.isEmpty()) {
            extension.check {
                minimumRate = extension.minimumRate.getOrElse(BigDecimal.valueOf(ScoverageExtension.DEFAULT_MINIMUM_RATE))
                coverageType = extension.coverageType.getOrElse(ScoverageExtension.DEFAULT_COVERAGE_TYPE)
            }
        } else if (extension.minimumRate.isPresent() || extension.coverageType.isPresent()) {
            throw new IllegalArgumentException("Check configuration should be defined in either the new or the old syntax exclusively, not together")
        }

        globalCheckTask.configure {
            group = 'verification'
            dependsOn globalReportTask
            onlyIf { scoverageReportDir.get().list() }
            reportDir.set(scoverageReportDir.get())
            checks.set(extension.checks.collect { config ->
                new CheckScoverageTask.CheckSpec(config.coverageType.configurationName, config.minimumRate)
            })
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

}
