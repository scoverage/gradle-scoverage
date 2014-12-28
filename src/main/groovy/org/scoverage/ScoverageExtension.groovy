package org.scoverage

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.Test

/**
 * Defines a new SourceSet for the code to be instrumented.
 * Defines a new Test Task which executes normal tests with the instrumented classes.
 * Defines a new Check Task which enforces an overall line coverage requirement.
 */
class ScoverageExtension {

    private static boolean isEscaped(String argument) {
        return (argument.startsWith('"') && argument.endsWith('"')) || (argument.startsWith('\'') && argument.endsWith('\''))
    }

    static String escape(String argument) {
        if (isEscaped(argument)) {
            return argument
        } else {
            return "\"$argument\""
        }
    }

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

    ScoverageExtension(Project project) {

        project.plugins.apply(JavaPlugin.class);
        project.plugins.apply(ScalaPlugin.class);
        project.afterEvaluate(configureRuntimeOptions)

        project.configurations.create(ScoveragePlugin.CONFIGURATION_NAME) {
            visible = false
            transitive = true
            description = 'Scoverage dependencies'
        }

        project.sourceSets.create(ScoveragePlugin.CONFIGURATION_NAME) {
            def mainSourceSet = project.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)

            java.source(mainSourceSet.java)
            scala.source(mainSourceSet.scala)

            compileClasspath += mainSourceSet.compileClasspath
            runtimeClasspath += mainSourceSet.runtimeClasspath
        }

        project.tasks.create(ScoveragePlugin.TEST_NAME, Test.class) {
            dependsOn(project.tasks[ScoveragePlugin.COMPILE_NAME])
        }

        project.tasks.create(ScoveragePlugin.REPORT_NAME, JavaExec.class) {
            dependsOn(project.tasks[ScoveragePlugin.TEST_NAME])
        }

        project.tasks.create(ScoveragePlugin.CHECK_NAME, OverallCheckTask.class) {
            dependsOn(project.tasks[ScoveragePlugin.REPORT_NAME])
        }

        dataDir = new File(project.buildDir, 'scoverage')
        reportDir = new File(project.buildDir, 'reports' + File.separatorChar + 'scoverage')
    }

    private Action<Project> configureRuntimeOptions = new Action<Project>() {

        @Override
        void execute(Project t) {

            def extension = ScoveragePlugin.extensionIn(t)
            extension.sources = t.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).scala.srcDirs.iterator().next() as File
            extension.dataDir.mkdirs()
            extension.reportDir.mkdirs()

            Configuration configuration = t.configurations[ScoveragePlugin.CONFIGURATION_NAME]
            File pluginFile
            try {
                pluginFile = configuration.filter { it.name.contains('plugin') }.iterator().next()
            } catch(NoSuchElementException e) {
                throw new GradleException("Could not find a plugin jar in configuration '${ScoveragePlugin.CONFIGURATION_NAME}'")
            }
            FileCollection pluginDependencies = configuration.filter { it != pluginFile }

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
                scalaCompileOptions.additionalParameters = parameters.collect { escape(it) }
                // exclude the scala libraries that are added to enable scala version detection
                classpath += pluginDependencies
            }

            t.tasks[ScoveragePlugin.TEST_NAME].configure {
                def existingClasspath = classpath
                classpath = t.files(t.sourceSets[ScoveragePlugin.CONFIGURATION_NAME].output.classesDir) +
                        pluginDependencies +
                        existingClasspath
            }

            t.tasks[ScoveragePlugin.REPORT_NAME].configure {
                classpath = project.buildscript.configurations.classpath + configuration
                main = 'org.scoverage.ScoverageReport'
                args = [
                        extension.sources,
                        extension.dataDir.absolutePath,
                        extension.reportDir.absolutePath
                ]
                inputs.dir(extension.dataDir)
                outputs.dir(extension.reportDir)
            }

        }
    }

}
