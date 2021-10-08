package org.scoverage;

import org.junit.Assert;
import org.junit.Test;

public class ScalaSingleModuleWithMultipleTestTasksTest extends ScoverageFunctionalTest {
    public ScalaSingleModuleWithMultipleTestTasksTest() {
        super("scala-single-module-multiple-test-tasks");
    }

    @Test
    public void test() {

        AssertableBuildResult result = dryRun("clean", "test");

        result.assertTaskDoesntExist(ScoveragePlugin.COMPILE_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.REPORT_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.AGGREGATE_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.CHECK_NAME);
    }

    @Test
    public void build() {

        AssertableBuildResult result = dryRun("clean", "build");

        result.assertTaskDoesntExist(ScoveragePlugin.COMPILE_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.REPORT_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.AGGREGATE_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.CHECK_NAME);
    }

    @Test
    public void reportScoverage() {

        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.REPORT_NAME);

        result.assertTaskExists(ScoveragePlugin.COMPILE_NAME);
        result.assertTaskExists(ScoveragePlugin.REPORT_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.AGGREGATE_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.CHECK_NAME);
    }

    @Test
    public void aggregateScoverage() {

        AssertableBuildResult result = runAndFail("clean", ScoveragePlugin.AGGREGATE_NAME);

        result.assertNoTasks();
    }


    @Test
    public void checkScoverage() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.CHECK_NAME);

        result.assertTaskSucceeded(ScoveragePlugin.COMPILE_NAME);
        result.assertTaskSucceeded("reportTestScoverage");
        result.assertTaskSucceeded("reportIntTestScoverage");
        result.assertTaskSucceeded(ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded(ScoveragePlugin.CHECK_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.AGGREGATE_NAME);

        assertReportFilesExist();
        assertCoverage(100.0);
    }

    @Test
    public void checkScoverageIntTest() throws Exception {
        AssertableBuildResult result = runAndFail("clean", "-x", "reportTestScoverage", ScoveragePlugin.CHECK_NAME);

        result.assertTaskSucceeded(ScoveragePlugin.COMPILE_NAME);
        result.assertTaskDoesntExist("reportTestScoverage");
        result.assertTaskSucceeded("reportIntTestScoverage");
        result.assertTaskSucceeded(ScoveragePlugin.REPORT_NAME);
        result.assertTaskFailed(ScoveragePlugin.CHECK_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.AGGREGATE_NAME);

        assertReportFilesExist();
        assertCoverage(50.0);
    }


    @Test
    public void checkScoverageFails() throws Exception {

        AssertableBuildResult result = runAndFail("clean", ScoveragePlugin.CHECK_NAME,
                "intTest", "--tests", "org.hello.TestNothingSuite",
                "-x", "test");

        result.assertTaskSucceeded(ScoveragePlugin.COMPILE_NAME);
        result.assertTaskSucceeded(ScoveragePlugin.REPORT_NAME);
        result.assertTaskFailed(ScoveragePlugin.CHECK_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.AGGREGATE_NAME);

        assertReportFilesExist();
        assertCoverage(0.0);
    }

    @Test
    public void reportScoverageWithExcludedClasses() throws Exception {

        AssertableBuildResult result = run("clean", "-PexcludedFile=.*", ScoveragePlugin.REPORT_NAME);

        result.assertTaskSucceeded(ScoveragePlugin.COMPILE_NAME);
        result.assertTaskSucceeded(ScoveragePlugin.REPORT_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.CHECK_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.AGGREGATE_NAME);

        Assert.assertTrue(resolve(reportDir(), "index.html").exists());
        Assert.assertFalse(resolve(reportDir(), "org/hello/World.scala.html").exists());
        assertCoverage(100.0); // coverage is 100 since no classes are covered

        // compiled class should exist in the default classes directory, but not in scoverage
        Assert.assertTrue(resolve(buildDir(), "classes/scala/main/org/hello/World.class").exists());
        Assert.assertFalse(resolve(buildDir(), "classes/scala/scoverage/org/hello/World.class").exists());
    }

    @Test
    public void reportScoverageWithoutNormalCompilation() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.REPORT_NAME,
                "-P" + ScoveragePlugin.SCOVERAGE_COMPILE_ONLY_PROPERTY);

        result.assertTaskSkipped("compileScala");
        result.assertTaskSucceeded(ScoveragePlugin.COMPILE_NAME);
        result.assertTaskSucceeded(ScoveragePlugin.REPORT_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.CHECK_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.AGGREGATE_NAME);

        assertReportFilesExist();
        assertCoverage(100.0);

        Assert.assertTrue(resolve(buildDir(), "classes/scala/main/org/hello/World.class").exists());
        Assert.assertFalse(resolve(buildDir(), "classes/scala/scoverage/org/hello/World.class").exists());
    }

    @Test
    public void reportScoverageWithoutNormalCompilationAndWithExcludedClasses() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.REPORT_NAME,
                "-PexcludedFile=.*", "-P" + ScoveragePlugin.SCOVERAGE_COMPILE_ONLY_PROPERTY);

        Assert.assertTrue(resolve(reportDir(), "index.html").exists());
        Assert.assertFalse(resolve(reportDir(), "org/hello/World.scala.html").exists());
        assertCoverage(100.0); // coverage is 100 since no classes are covered

        // compiled class should exist in the default classes directory, but not in scoverage
        Assert.assertTrue(resolve(buildDir(), "classes/scala/main/org/hello/World.class").exists());
        Assert.assertFalse(resolve(buildDir(), "classes/scala/scoverage/org/hello/World.class").exists());
    }


    private void assertReportFilesExist() {

        Assert.assertTrue(resolve(reportDir(), "index.html").exists());
        Assert.assertTrue(resolve(reportDir(), "org/hello/World.scala.html").exists());
    }
}
