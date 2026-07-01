package org.scoverage

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.Test

class ScoverageTestSupport {

    static String scoverageTestTaskName(String testTaskName) {
        return "scoverage${testTaskName.capitalize()}"
    }

    static boolean isScoverageTestTask(String taskName) {
        return taskName.startsWith('scoverage') && taskName.length() > 'scoverage'.length()
    }

    static void registerScoverageTest(Project project,
                                      Test testTask,
                                      SourceSet instrumentedSourceSet,
                                      String scoverageConfigurationName,
                                      def compileTask,
                                      org.gradle.api.provider.Property<File> dataDir) {
        def scoverageTestName = scoverageTestTaskName(testTask.name)
        if (project.tasks.names.contains(scoverageTestName)) {
            return
        }

        project.tasks.register(scoverageTestName, Test) { scoverageTest ->
            scoverageTest.group = 'verification'
            scoverageTest.description = "Runs ${testTask.name} with scoverage instrumented classes."
            scoverageTest.testClassesDirs.from(testTask.testClassesDirs)
            scoverageTest.classpath.from(
                    project.configurations.named(scoverageConfigurationName),
                    instrumentedSourceSet.output,
                    testTask.classpath
            )
            scoverageTest.javaLauncher.convention(testTask.javaLauncher)
            scoverageTest.jvmArgs.addAll(testTask.jvmArgs)
            scoverageTest.systemProperties.putAll(testTask.systemProperties)
            scoverageTest.environment.putAll(testTask.environment)
            scoverageTest.maxParallelForks = testTask.maxParallelForks
            scoverageTest.forkEvery = testTask.forkEvery
            scoverageTest.maxHeapSize = testTask.maxHeapSize
            scoverageTest.debug = testTask.debug
            scoverageTest.enableAssertions = testTask.enableAssertions
            if (testTask.options instanceof org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions) {
                scoverageTest.useJUnitPlatform()
            }
            scoverageTest.mustRunAfter(compileTask)
            scoverageTest.outputs.upToDateWhen {
                dataDir.get().listFiles(new FilenameFilter() {
                    @Override
                    boolean accept(File dir, String name) {
                        return name.startsWith("scoverage.measurements.")
                    }
                })
            }
        }
    }
}
