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
import java.util.ArrayList;
import java.util.Arrays;
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
        return ScalaVersionArguments.version2;
    }

    private void configureArguments(String... arguments) {

        List<String> fullArguments = new ArrayList<>(getVersionAgruments());

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
    }
}

