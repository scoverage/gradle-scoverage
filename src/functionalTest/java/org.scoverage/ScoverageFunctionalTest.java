package org.scoverage;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ScoverageFunctionalTest {

    private final String projectName;
    private final GradleRunner runner;

    protected ScoverageFunctionalTest(String projectName) {

        this.projectName = projectName;
        this.runner = GradleRunner.create()
                .withProjectDir(projectDir())
                .withPluginClasspath()
                .forwardOutput();


        List<File> filteredPluginClassPath = new ArrayList<File>();

        for (File file : runner.getPluginClasspath()) {
            if (!file.getName().contains("scalac-scoverage-plugin")) {
                filteredPluginClassPath.add(file);
            }
        }

        runner.withPluginClasspath(filteredPluginClassPath);
    }

    protected File projectDir() {

        return new File("src/functionalTest/resources/projects/" + projectName);
    }

    protected AssertableBuildResult run(String... arguments) {

        configureArguments(arguments);
        return new AssertableBuildResult(runner.build());
    }

    protected AssertableBuildResult runAndFail(String... arguments) {

        configureArguments(arguments);
        return new AssertableBuildResult(runner.buildAndFail());
    }

    protected AssertableBuildResult dryRun(String... arguments) {

        List<String> withDryArgument = new ArrayList<String>(Arrays.asList(arguments));
        withDryArgument.add("--dry-run");
        return run(withDryArgument.toArray(new String[]{}));
    }

    private void configureArguments(String... arguments) {

        List<String> fullArguments = new ArrayList<String>();

        fullArguments.add("-PscalaVersionMajor=2");
        fullArguments.add("-PscalaVersionMinor=11");
        fullArguments.add("-PscalaVersionBuild=5");
        fullArguments.add("-PjunitVersion=5.3.2");
        fullArguments.add("-PjunitPlatformVersion=1.3.2");
        fullArguments.add("-PscalatestVersion=3.0.5");
        fullArguments.add("-PscoverageVersion=1.3.1");
        fullArguments.addAll(Arrays.asList(arguments));

        runner.withArguments(fullArguments);
    }

    protected static class AssertableBuildResult {

        private final BuildResult result;

        private AssertableBuildResult(BuildResult result) {

            this.result = result;
        }

        public BuildResult getResult() {

            return result;
        }

        public void assertNoTasks() {

            Assert.assertEquals(0, result.getTasks().size());
        }

        public void assertTaskExists(String taskName) {

            Assert.assertTrue(taskExists(taskName));
        }

        public void assertTaskDoesntExist(String taskName) {

            Assert.assertFalse(taskExists(taskName));
        }

        public void assertTaskSkipped(String taskName) {

            BuildTask task = getTask(taskName);
            Assert.assertTrue(task == null || task.getOutcome() == TaskOutcome.SKIPPED);
        }

        public void assertTaskOutcome(String taskName, TaskOutcome outcome) {

            BuildTask task = getTask(taskName);
            Assert.assertNotNull(task);
            Assert.assertEquals(outcome, task.getOutcome());

        }

        private BuildTask getTask(String taskName) {

            return result.task(fullTaskName(taskName));
        }

        private String fullTaskName(String taskName) {

            return ":" + taskName;
        }

        private boolean taskExists(String taskName) {

            return result.getOutput().contains(fullTaskName(taskName) + " ");
        }
    }
}

