package com.github.maiflai

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.Test

class ScctExtension {

    ScctExtension(Project project) {

        project.plugins.apply(JavaPlugin.class);
        project.plugins.apply(ScalaPlugin.class);
        project.afterEvaluate(configureRuntimeOptions)

        project.configurations.create(ScctPlugin.CONFIGURATION_NAME) {
            visible = false
            transitive = false
            description = 'SCCT dependencies'
        }

        project.sourceSets.create(ScctPlugin.CONFIGURATION_NAME) {
            def mainSourceSet = project.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)

            java.source(mainSourceSet.java)
            scala.source(mainSourceSet.scala)

            compileClasspath += mainSourceSet.compileClasspath
            runtimeClasspath += mainSourceSet.runtimeClasspath
        }

        project.tasks.create(ScctPlugin.TEST_NAME, Test.class) {
            dependsOn(project.tasks[ScctPlugin.COMPILE_NAME])
        }

        project.tasks.create(ScctPlugin.CHECK_NAME, OverallCheckTask.class) {
            dependsOn(project.tasks[ScctPlugin.TEST_NAME])
        }

    }

    private Action<Project> configureRuntimeOptions = new Action<Project>() {

        @Override
        void execute(Project t) {
            t.tasks[ScctPlugin.COMPILE_NAME].configure {
                List<String> plugin = ['-Xplugin:' + t.configurations[ScctPlugin.CONFIGURATION_NAME].singleFile]
                List<String> parameters = scalaCompileOptions.additionalParameters
                if (parameters != null) {
                    plugin.addAll(parameters)
                }
                scalaCompileOptions.additionalParameters = plugin
                // exclude the scala libraries that are added to enable scala version detection
                classpath += t.configurations[ScctPlugin.CONFIGURATION_NAME]
            }
            t.tasks[ScctPlugin.TEST_NAME].configure {
                systemProperty 'scct.report.dir', "${t.buildDir}/reports/${t.extensions[ScctPlugin.CONFIGURATION_NAME].reportDirName}"
                def existingClasspath = classpath
                classpath = t.files(t.sourceSets[ScctPlugin.CONFIGURATION_NAME].output.classesDir) +
                        project.configurations[ScctPlugin.CONFIGURATION_NAME] +
                        existingClasspath
            }
        }
    }

    String reportDirName = 'scct'


}


