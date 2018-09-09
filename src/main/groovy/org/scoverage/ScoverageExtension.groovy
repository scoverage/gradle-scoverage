package org.scoverage

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.util.GFileUtils

import java.util.concurrent.Callable

/**
 * Defines a new SourceSet for the code to be instrumented.
 * Defines a new Test Task which executes normal tests with the instrumented classes.
 * Defines a new Check Task which enforces an overall line coverage requirement.
 */
class ScoverageExtension {

    /** a directory to write working files to */
    File dataDir
    /** a directory to write final output to */
    File reportDir
    /** sources to highlight */
    File sources
    /** range positioning for highlighting */
    boolean highlighting = true
    /** regex for each excluded package */
    List<String> excludedPackages = []
    /** regex for each excluded file */
    List<String> excludedFiles = []

    FileCollection pluginClasspath

    /** Options for enabling and disabling output */
    boolean coverageOutputCobertura = true
    boolean coverageOutputXML = true
    boolean coverageOutputHTML = true
    boolean coverageDebug = false

    ScoverageExtension(Project project) {

        project.plugins.apply(JavaPlugin.class)
        project.plugins.apply(ScalaPlugin.class)
        project.afterEvaluate(configureRuntimeOptions)

        project.configurations.create(ScoveragePlugin.CONFIGURATION_NAME) {
            visible = false
            transitive = true
            description = 'Scoverage dependencies'
        }

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
            classifier = ScoveragePlugin.CONFIGURATION_NAME
            from instrumentedSourceSet.output
        }
        project.artifacts {
            scoverage scoverageJar
        }

        project.tasks.create(ScoveragePlugin.TEST_NAME, Test.class) {
            conventionMapping.map("classpath", new Callable<Object>() {
                Object call() throws Exception {
                    def testSourceSet = project.sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)
                    return testSourceSet.output +
                            instrumentedSourceSet.output +
                            project.configurations.scoverage +
                            testSourceSet.runtimeClasspath
                }
            })
        }

        project.tasks.create(ScoveragePlugin.REPORT_NAME, ScoverageReport.class) {
            dependsOn(project.tasks[ScoveragePlugin.TEST_NAME])
            onlyIf { ScoveragePlugin.extensionIn(project).dataDir.list() }
        }

        project.tasks.create(ScoveragePlugin.CHECK_NAME, OverallCheckTask.class) {
            dependsOn(project.tasks[ScoveragePlugin.REPORT_NAME])
        }

        sources = project.projectDir
        dataDir = new File(project.buildDir, 'scoverage')
        reportDir = new File(project.buildDir, 'reports' + File.separatorChar + 'scoverage')
        def classLocation = ScoverageExtension.class.getProtectionDomain().getCodeSource().getLocation()
        pluginClasspath = project.files(classLocation.file) + project.configurations.scoverage
    }

    private Action<Project> configureRuntimeOptions = new Action<Project>() {

        @Override
        void execute(Project t) {

            def extension = ScoveragePlugin.extensionIn(t)
            extension.dataDir.mkdirs()

            Configuration configuration = t.configurations[ScoveragePlugin.CONFIGURATION_NAME]
            File pluginFile
            try {
                pluginFile = configuration.filter { it.name.contains('plugin') }.iterator().next()
            } catch(NoSuchElementException ignored) {
                throw new GradleException("Could not find a plugin jar in configuration '${ScoveragePlugin.CONFIGURATION_NAME}'")
            }

            t.tasks[ScoveragePlugin.COMPILE_NAME].configure {
                List<String> parameters = ['-Xplugin:' + pluginFile.absolutePath]
                List<String> existingParameters = scalaCompileOptions.additionalParameters
                if (existingParameters) {
                    parameters.addAll(existingParameters)
                }
                parameters.add("-P:scoverage:dataDir:${extension.dataDir.absolutePath}".toString())
                if (extension.excludedPackages) {
                    parameters.add("-P:scoverage:excludedPackages:${extension.excludedPackages.join(';')}".toString())
                }
                if (extension.excludedFiles) {
                    parameters.add("-P:scoverage:excludedFiles:${extension.excludedFiles.join(';')}".toString())
                }
                if (extension.highlighting) {
                    parameters.add('-Yrangepos')
                }
                doFirst {
                    GFileUtils.deleteDirectory(destinationDir)
                }
                scalaCompileOptions.additionalParameters = parameters
                // the compile task creates a store of measured statements
                outputs.file(new File(extension.dataDir, 'scoverage.coverage.xml'))
            }
            t.tasks[ScoveragePlugin.TEST_NAME].outputs.files("${extension.dataDir}/scoverage.measurements.*")
            t.tasks[ScoveragePlugin.REPORT_NAME].configure {
                inputs.dir(extension.dataDir)
                outputs.dir(extension.reportDir)
            }
        }
    }
}
