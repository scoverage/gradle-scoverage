package org.scoverage;

import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class ScalaJavaAnnotationProcessorTest extends ScoverageFunctionalTest {

    public ScalaJavaAnnotationProcessorTest() {
        super("scala-java-annotation-processor");
    }

    @Test
    public void checkAndAggregateScoverage() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.CHECK_NAME,
                ScoveragePlugin.AGGREGATE_NAME);

        result.assertTaskSkipped("java_only:" + ScoveragePlugin.COMPILE_NAME);

        result.assertTaskSkipped(ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded("mixed_scala_java:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskSkipped("java_only:" + ScoveragePlugin.REPORT_NAME);

        result.assertTaskSucceeded(ScoveragePlugin.CHECK_NAME);
        result.assertTaskSucceeded("mixed_scala_java:" + ScoveragePlugin.CHECK_NAME);
        result.assertTaskSkipped("java_only:" + ScoveragePlugin.CHECK_NAME);

        result.assertTaskSucceeded(ScoveragePlugin.AGGREGATE_NAME);

        assertAllReportFilesExist();
        assertCoverage(100.0);
    }

    private void assertAllReportFilesExist() {

        Assert.assertTrue(resolve(reportDir(), "index.html").exists());

        assertMixedScalaJavaReportFilesExist();
        assertAggregationFilesExist();
    }

    private void assertAggregationFilesExist() {

        Assert.assertTrue(resolve(reportDir(), "org/hello/WorldScala.scala.html").exists());
    }

    private void assertMixedScalaJavaReportFilesExist() {

        File reportDir = reportDir(projectDir().toPath().resolve("mixed_scala_java").toFile());
        Assert.assertTrue(resolve(reportDir, "index.html").exists());
        Assert.assertTrue(resolve(reportDir, "org/hello/WorldScala.scala.html").exists());
    }
}
