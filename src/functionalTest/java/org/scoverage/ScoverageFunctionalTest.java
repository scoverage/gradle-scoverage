package org.scoverage;

import groovy.util.Node;
import groovy.xml.XmlParser;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Assert;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;

public abstract class ScoverageFunctionalTest {

    private String projectName;
    private GradleRunner runner;
    private final XmlParser parser;

    protected ScoverageFunctionalTest(String projectName) {
        setProjectName(projectName);
        try {
            this.parser = new XmlParser();
            parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void setProjectName(String projectName) {
        if (projectName != null) {
            this.projectName = projectName;
            this.runner = GradleRunner.create()
                    .withProjectDir(projectDir())
                    .withPluginClasspath()
                    .forwardOutput();
        }
    }

    protected File projectDir() {

        return new File(getClass().getClassLoader().getResource("projects/" + projectName).getFile());
    }

    protected File buildDir() {

        return buildDir(projectDir());
    }

    protected File buildDir(File projectDir) {

        return projectDir.toPath().resolve("build").toFile();
    }

    protected File reportDir() {

        return reportDir(projectDir());
    }

    protected File reportDir(File projectDir) {

        return buildDir(projectDir).toPath().resolve(ScoveragePlugin.getDEFAULT_REPORT_DIR()).toFile();
    }

    protected AssertableBuildResult run(String... arguments) {

        configureArguments(arguments);
        return new AssertableBuildResult(runner.build());
    }

    protected AssertableBuildResult runWithConfigurationCache(String... arguments) {

        configureConfigurationCacheArguments(arguments);
        return new AssertableBuildResult(runner.build());
    }

    protected void prepareConfigurationCacheRun(String... arguments) {

        configureConfigurationCacheArguments(arguments);
    }

    protected AssertableBuildResult buildPreparedConfigurationCacheRun() {

        return new AssertableBuildResult(runner.build());
    }

    protected void cleanProjectForConfigurationCache() throws IOException {

        deleteRecursively(buildDir());
        deleteRecursively(new File(projectDir(), ".gradle"));
    }

    private void deleteRecursively(File directory) throws IOException {

        if (!directory.exists()) {
            return;
        }

        Files.walk(directory.toPath())
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    protected AssertableBuildResult runAndFail(String... arguments) {

        configureArguments(arguments);
        return new AssertableBuildResult(runner.buildAndFail());
    }

    protected AssertableBuildResult dryRun(String... arguments) {

        List<String> withDryArgument = new ArrayList<>(Arrays.asList(arguments));
        withDryArgument.add("--dry-run");
        return run(withDryArgument.toArray(new String[]{}));
    }

    protected void assertCoverage(Double expected) throws Exception {

        assertCoverage(expected, reportDir());
    }

    protected void assertCoverage(Double expected, File reportDir) throws Exception {

        assertThat(coverage(reportDir, CoverageType.Statement), closeTo(expected, 1.0));
        assertThat(coverage(reportDir, CoverageType.Line), closeTo(expected, 1.0));
    }

    protected File resolve(File file, String relativePath) {

        return file.toPath().resolve(relativePath).toFile();
    }

    private Double coverage(File reportDir, CoverageType coverageType) throws IOException, SAXException, NumberFormatException {

        File reportFile = reportDir.toPath().resolve(coverageType.getFileName()).toFile();
        Node xml = parser.parse(reportFile);
        Object attribute = xml.attribute(coverageType.getParamName());
        double rawValue = Double.parseDouble(attribute.toString());
        return coverageType.normalize(rawValue) * 100.0;
    }

    protected List<String> getVersionAgruments() {
        return ScalaVersionArguments.version2WithLegacyScalatest;
    }

    private void configureArguments(String... arguments) {

        configureArguments(false, arguments);
    }

    private void configureConfigurationCacheArguments(String... arguments) {

        configureArguments(true, arguments);
    }

    private void configureArguments(boolean configurationCache, String... arguments) {

        List<String> fullArguments = new ArrayList<>(getVersionAgruments());

        if (configurationCache) {
            fullArguments.add("--configuration-cache");
            fullArguments.add("--configuration-cache-problems=fail");
        }

        if (Boolean.parseBoolean(System.getProperty("failOnWarning"))) {
            fullArguments.add("--warning-mode=fail");
        } else {
            fullArguments.add("--warning-mode=all");
        }
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

        public void assertTaskSucceeded(String taskName) {

            assertTaskOutcome(taskName, TaskOutcome.SUCCESS);
        }

        public void assertTaskFailed(String taskName) {

            assertTaskOutcome(taskName, TaskOutcome.FAILED);
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

            Pattern regex = Pattern.compile("^(> Task )?" + fullTaskName(taskName), Pattern.MULTILINE);
            return regex.matcher(result.getOutput()).find();
        }

        public void assertConfigurationCacheStored() {

            Assert.assertTrue(
                    "Expected configuration cache to be stored",
                    result.getOutput().contains("Configuration cache entry stored")
                            || result.getOutput().contains("Calculating task graph as no cached configuration is available")
            );
        }

        public void assertConfigurationCacheReused() {

            String output = result.getOutput();
            Assert.assertTrue(
                    "Expected configuration cache to be reused",
                    output.contains("Reusing configuration cache")
                            || output.contains("Configuration cache entry reused")
                            || output.contains("from configuration cache")
            );
        }
    }
}

