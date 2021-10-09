package org.scoverage;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class ScalaJavaMultiModuleTest extends ScoverageFunctionalTest {

    public ScalaJavaMultiModuleTest() {
        super("scala-java-multi-module");
    }

    @Test
    public void checkAndAggregateScoverage() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.CHECK_NAME,
                ScoveragePlugin.AGGREGATE_NAME);

        result.assertTaskSkipped("java_only:" + ScoveragePlugin.COMPILE_NAME);

        result.assertTaskSkipped(ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded("scala_only:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded("mixed_scala_java:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskSkipped("java_only:" + ScoveragePlugin.REPORT_NAME);

        result.assertTaskSucceeded(ScoveragePlugin.CHECK_NAME);
        result.assertTaskSucceeded("scala_only:" + ScoveragePlugin.CHECK_NAME);
        result.assertTaskSucceeded("mixed_scala_java:" + ScoveragePlugin.CHECK_NAME);
        result.assertTaskSkipped("java_only:" + ScoveragePlugin.CHECK_NAME);

        result.assertTaskSucceeded(ScoveragePlugin.AGGREGATE_NAME);

        assertAllReportFilesExist();
        assertCoverage(100.0);
    }

    private void assertAllReportFilesExist() {

        Assert.assertTrue(resolve(reportDir(), "index.html").exists());

        assertScalaOnlyReportFilesExist();
        assertMixedScalaJavaReportFilesExist();
        assertAggregationFilesExist();
    }

    private void assertAggregationFilesExist() {

        Assert.assertTrue(resolve(reportDir(), "org/hello/WorldScalaOnly.scala.html").exists());
        Assert.assertTrue(resolve(reportDir(), "org/hello/WorldScala.scala.html").exists());
    }

    private void assertScalaOnlyReportFilesExist() {

        File reportDir = reportDir(projectDir().toPath().resolve("scala_only").toFile());
        Assert.assertTrue(resolve(reportDir, "index.html").exists());
        Assert.assertTrue(resolve(reportDir, "org/hello/WorldScalaOnly.scala.html").exists());
    }

    private void assertMixedScalaJavaReportFilesExist() {

        File reportDir = reportDir(projectDir().toPath().resolve("mixed_scala_java").toFile());
        Assert.assertTrue(resolve(reportDir, "index.html").exists());
        Assert.assertTrue(resolve(reportDir, "org/hello/WorldScala.scala.html").exists());
    }
}
