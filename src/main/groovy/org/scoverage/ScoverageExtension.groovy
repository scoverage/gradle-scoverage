package org.scoverage

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.Test

/**
 * Defines a new SourceSet for the code to be instrumented.
 * Defines a new Test Task which executes normal tests with the instrumented classes.
 * Defines a new Check Task which enforces an overall line coverage requirement.
 */
class ScoverageExtension {

    ScoverageExtension(Project project) {

        project.plugins.apply(JavaPlugin.class);
        project.plugins.apply(ScalaPlugin.class);
        project.afterEvaluate(configureRuntimeOptions)

        project.configurations.create(ScoveragePlugin.CONFIGURATION_NAME) {
            visible = false
            transitive = false
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

        project.tasks.create(ScoveragePlugin.CHECK_NAME, OverallCheckTask.class) {
            dependsOn(project.tasks[ScoveragePlugin.TEST_NAME])
        }

    }

    private Action<Project> configureRuntimeOptions = new Action<Project>() {

        @Override
        void execute(Project t) {
            t.tasks[ScoveragePlugin.COMPILE_NAME].configure {
                List<String> plugin = ['-Xplugin:' + t.configurations[ScoveragePlugin.CONFIGURATION_NAME].singleFile]
                List<String> parameters = scalaCompileOptions.additionalParameters
                if (parameters != null) {
                    plugin.addAll(parameters)
                }
                scalaCompileOptions.additionalParameters = plugin
                // exclude the scala libraries that are added to enable scala version detection
                classpath += t.configurations[ScoveragePlugin.CONFIGURATION_NAME]
            }
            t.tasks[ScoveragePlugin.TEST_NAME].configure {
                // TODO : fix this
                systemProperty 'scoverage.report.dir', "${t.buildDir}/reports/${t.extensions[ScoveragePlugin.CONFIGURATION_NAME].reportDirName}"
                systemProperty 'scoverage.basedir', "${t.rootDir.absolutePath}"  // for multi-module checking

                def existingClasspath = classpath
                classpath = t.files(t.sourceSets[ScoveragePlugin.CONFIGURATION_NAME].output.classesDir) +
                        project.configurations[ScoveragePlugin.CONFIGURATION_NAME] +
                        existingClasspath
            }
        }
    }

    String reportDirName = 'scoverage'
}
