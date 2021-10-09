package org.scoverage;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class ScalaMultiModuleTest extends ScoverageFunctionalTest {

    public ScalaMultiModuleTest() {
        super("scala-multi-module");
    }

    @Test
    public void reportScoverage() {

        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.REPORT_NAME);

        result.assertTaskExists(ScoveragePlugin.REPORT_NAME);
        result.assertTaskExists("a:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskExists("b:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskExists("common:" + ScoveragePlugin.REPORT_NAME);
    }

    @Test
    public void reportScoverageParallel() {

        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.REPORT_NAME, "--parallel");

        result.assertTaskExists(ScoveragePlugin.REPORT_NAME);
        result.assertTaskExists("a:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskExists("b:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskExists("common:" + ScoveragePlugin.REPORT_NAME);
    }

    @Test
    public void reportScoverageOnlyRoot() {

        AssertableBuildResult result = dryRun("clean", ":" + ScoveragePlugin.REPORT_NAME);

        result.assertTaskExists(ScoveragePlugin.REPORT_NAME);
        result.assertTaskDoesntExist("a:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskDoesntExist("b:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskDoesntExist("common:" + ScoveragePlugin.REPORT_NAME);
    }

    @Test
    public void reportScoverageOnlyA() {

        AssertableBuildResult result = run("clean", ":a:" + ScoveragePlugin.REPORT_NAME);

        result.assertTaskDoesntExist(ScoveragePlugin.REPORT_NAME);
        result.assertTaskDoesntExist("b:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskDoesntExist("common:" + ScoveragePlugin.REPORT_NAME);

        result.assertTaskSucceeded("a:" + ScoveragePlugin.COMPILE_NAME);
        result.assertTaskSucceeded("a:" + ScoveragePlugin.REPORT_NAME);

        assertAReportFilesExist();
    }

    @Test
    public void reportScoverageOnlyAWithoutNormalCompilation() {

        AssertableBuildResult result = run("clean", ":a:" + ScoveragePlugin.REPORT_NAME,
                "-P" + ScoveragePlugin.SCOVERAGE_COMPILE_ONLY_PROPERTY);

        result.assertTaskSkipped("compileScala");
        result.assertTaskSkipped("a:compileScala");
        result.assertTaskSkipped("common:compileScala");
        result.assertTaskSucceeded("common:" + ScoveragePlugin.COMPILE_NAME);
        result.assertTaskSucceeded("a:" + ScoveragePlugin.COMPILE_NAME);
        result.assertTaskSucceeded("a:" + ScoveragePlugin.REPORT_NAME);

        assertAReportFilesExist();

        Assert.assertTrue(resolve(buildDir(resolve(projectDir(), "a")), "classes/scala/main/org/hello/a/WorldA.class").exists());
        Assert.assertFalse(resolve(buildDir(resolve(projectDir(), "a")), "classes/scala/scoverage/org/hello/a/WorldA.class").exists());

        Assert.assertTrue(resolve(buildDir(resolve(projectDir(), "common")), "classes/scala/main/org/hello/common/WorldCommon.class").exists());
        Assert.assertFalse(resolve(buildDir(resolve(projectDir(), "common")), "classes/scala/scoverage/org/hello/common/WorldCommon.class").exists());
    }

    @Test
    public void aggregateScoverage() {

        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.AGGREGATE_NAME);

        result.assertTaskExists(ScoveragePlugin.REPORT_NAME);
        result.assertTaskExists("a:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskExists("b:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskExists(ScoveragePlugin.AGGREGATE_NAME);
        result.assertTaskExists("common:" + ScoveragePlugin.REPORT_NAME);
    }

    @Test
    public void checkScoverage() {

        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.CHECK_NAME);

        result.assertTaskExists(ScoveragePlugin.REPORT_NAME);
        result.assertTaskExists("a:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskExists("b:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskExists("common:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskExists(ScoveragePlugin.CHECK_NAME);
        result.assertTaskExists("a:" + ScoveragePlugin.CHECK_NAME);
        result.assertTaskExists("b:" + ScoveragePlugin.CHECK_NAME);
        result.assertTaskExists("common:" + ScoveragePlugin.CHECK_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.AGGREGATE_NAME);
    }

    @Test
    public void checkScoverageOnlyRoot() {

        AssertableBuildResult result = dryRun("clean", ":" + ScoveragePlugin.CHECK_NAME);

        result.assertTaskExists(ScoveragePlugin.REPORT_NAME);
        result.assertTaskDoesntExist("a:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskDoesntExist("b:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskExists(ScoveragePlugin.CHECK_NAME);
        result.assertTaskDoesntExist("a:" + ScoveragePlugin.CHECK_NAME);
        result.assertTaskDoesntExist("b:" + ScoveragePlugin.CHECK_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.AGGREGATE_NAME);
    }

    @Test
    public void checkScoverageOnlyA() {

        AssertableBuildResult result = dryRun("clean", ":a:" + ScoveragePlugin.CHECK_NAME);

        result.assertTaskDoesntExist(ScoveragePlugin.REPORT_NAME);
        result.assertTaskExists("a:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskDoesntExist("b:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskDoesntExist("common:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.CHECK_NAME);
        result.assertTaskExists("a:" + ScoveragePlugin.CHECK_NAME);
        result.assertTaskDoesntExist("b:" + ScoveragePlugin.CHECK_NAME);
        result.assertTaskDoesntExist("common:" + ScoveragePlugin.CHECK_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.AGGREGATE_NAME);
    }

    @Test
    public void checkAndAggregateScoverage() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.CHECK_NAME,
                ScoveragePlugin.AGGREGATE_NAME);

        result.assertTaskSucceeded(ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded("a:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded("b:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded("common:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded(ScoveragePlugin.CHECK_NAME);
        result.assertTaskSucceeded("a:" + ScoveragePlugin.CHECK_NAME);
        result.assertTaskSucceeded("b:" + ScoveragePlugin.CHECK_NAME);
        result.assertTaskSucceeded("common:" + ScoveragePlugin.CHECK_NAME);
        result.assertTaskSucceeded(ScoveragePlugin.AGGREGATE_NAME);

        assertAllReportFilesExist();
        assertCoverage(100.0);
    }

    @Test
    public void checkScoverageWithoutCoverageInRoot() throws Exception {

        AssertableBuildResult result = runAndFail("clean", ScoveragePlugin.CHECK_NAME,
                "test",
                "--tests", "org.hello.TestNothingSuite",
                "--tests", "org.hello.a.WorldASuite",
                "--tests", "org.hello.b.WorldBSuite",
                "--tests", "org.hello.common.WorldCommonSuite");

        result.assertTaskFailed(ScoveragePlugin.CHECK_NAME);

        assertRootReportFilesExist();
        assertCoverage(0.0);
    }

    @Test
    public void checkScoverageWithoutCoverageInA() throws Exception {

        AssertableBuildResult result = runAndFail("clean", ScoveragePlugin.CHECK_NAME,
                "test",
                "--tests", "org.hello.a.TestNothingASuite",
                "--tests", "org.hello.WorldSuite",
                "--tests", "org.hello.b.WorldBSuite",
                "--tests", "org.hello.common.WorldCommonSuite");

        result.assertTaskFailed("a:" + ScoveragePlugin.CHECK_NAME);

        assertAReportFilesExist();
        assertCoverage(0.0, reportDir(projectDir().toPath().resolve("a").toFile()));
    }

    @Test
    public void checkScoverageWithoutNormalCompilationAndWithoutCoverageInCommon() throws Exception {

        AssertableBuildResult result = runAndFail("clean",
                ":a:test",
                ":common:test", "--tests", "org.hello.common.TestNothingCommonSuite",
                "-P" + ScoveragePlugin.SCOVERAGE_COMPILE_ONLY_PROPERTY,
                ScoveragePlugin.CHECK_NAME);

        result.assertTaskFailed("common:" + ScoveragePlugin.CHECK_NAME);

        assertCommonReportFilesExist();
        assertCoverage(0.0, reportDir(projectDir().toPath().resolve("common").toFile()));
    }

    @Test
    public void checkAndAggregateScoverageWithoutCoverageInRoot() throws Exception {

        // should pass as the check on the root is for the aggregation (which covers > 50%)

        AssertableBuildResult result = run("clean", ScoveragePlugin.CHECK_NAME,
                ScoveragePlugin.AGGREGATE_NAME, "test",
                "--tests", "org.hello.TestNothingSuite",
                "--tests", "org.hello.a.WorldASuite",
                "--tests", "org.hello.b.WorldBSuite",
                "--tests", "org.hello.common.WorldCommonSuite");

        result.assertTaskSucceeded(ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded("a:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded("b:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded("common:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded(ScoveragePlugin.CHECK_NAME);
        result.assertTaskSucceeded("a:" + ScoveragePlugin.CHECK_NAME);
        result.assertTaskSucceeded("b:" + ScoveragePlugin.CHECK_NAME);
        result.assertTaskSucceeded("common:" + ScoveragePlugin.CHECK_NAME);
        result.assertTaskSucceeded(ScoveragePlugin.AGGREGATE_NAME);

        assertAllReportFilesExist();
        assertCoverage(87.5);
    }

    @Test
    public void checkAndAggregateScoverageWithoutCoverageInAll() throws Exception {

        AssertableBuildResult result = runAndFail("clean", ScoveragePlugin.CHECK_NAME,
                ScoveragePlugin.AGGREGATE_NAME, "test",
                "--tests", "org.hello.TestNothingSuite",
                "--tests", "org.hello.a.TestNothingASuite",
                "--tests", "org.hello.b.TestNothingBSuite",
                "--tests", "org.hello.common.TestNothingCommonSuite");

        result.assertTaskSucceeded(ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded("a:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded("b:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded("common:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded(ScoveragePlugin.AGGREGATE_NAME);
        result.assertTaskFailed(ScoveragePlugin.CHECK_NAME);

        assertAllReportFilesExist();
        assertCoverage(0.0);
    }

    @Test
    public void aggregateScoverageWithoutNormalCompilation() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.AGGREGATE_NAME,
                "-P" + ScoveragePlugin.SCOVERAGE_COMPILE_ONLY_PROPERTY);

        result.assertTaskSkipped("compileScala");
        result.assertTaskSkipped("a:compileScala");
        result.assertTaskSkipped("b:compileScala");
        result.assertTaskSkipped("common:compileScala");
        result.assertTaskSucceeded(ScoveragePlugin.COMPILE_NAME);
        result.assertTaskSucceeded("a:" + ScoveragePlugin.COMPILE_NAME);
        result.assertTaskSucceeded("b:" + ScoveragePlugin.COMPILE_NAME);
        result.assertTaskSucceeded("common:" + ScoveragePlugin.COMPILE_NAME);
        result.assertTaskSucceeded(ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded("a:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded("b:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded("common:" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded(ScoveragePlugin.AGGREGATE_NAME);

        assertAllReportFilesExist();
        assertCoverage(100.0);

        Assert.assertTrue(resolve(buildDir(resolve(projectDir(), "a")), "classes/scala/main/org/hello/a/WorldA.class").exists());
        Assert.assertFalse(resolve(buildDir(resolve(projectDir(), "a")), "classes/scala/scoverage/org/hello/a/WorldA.class").exists());

        Assert.assertTrue(resolve(buildDir(resolve(projectDir(), "b")), "classes/scala/main/org/hello/b/WorldB.class").exists());
        Assert.assertFalse(resolve(buildDir(resolve(projectDir(), "b")), "classes/scala/scoverage/org/hello/b/WorldB.class").exists());

        Assert.assertTrue(resolve(buildDir(resolve(projectDir(), "common")), "classes/scala/main/org/hello/common/WorldCommon.class").exists());
        Assert.assertFalse(resolve(buildDir(resolve(projectDir(), "common")), "classes/scala/scoverage/org/hello/common/WorldCommon.class").exists());
    }

    private void assertAllReportFilesExist() {

        assertRootReportFilesExist();
        assertAReportFilesExist();
        assertBReportFilesExist();
        assertCommonReportFilesExist();
        assertAggregationFilesExist();
    }

    private void assertAggregationFilesExist() {

        Assert.assertTrue(resolve(reportDir(), "org/hello/a/WorldA.scala.html").exists());
        Assert.assertTrue(resolve(reportDir(), "org/hello/b/WorldB.scala.html").exists());
        Assert.assertTrue(resolve(reportDir(), "org/hello/common/WorldCommon.scala.html").exists());
    }

    private void assertRootReportFilesExist() {

        Assert.assertTrue(resolve(reportDir(), "index.html").exists());
        Assert.assertTrue(resolve(reportDir(), "org/hello/World.scala.html").exists());
    }

    private void assertAReportFilesExist() {

        File reportDir = reportDir(projectDir().toPath().resolve("a").toFile());
        Assert.assertTrue(resolve(reportDir, "index.html").exists());
        Assert.assertTrue(resolve(reportDir, "org/hello/a/WorldA.scala.html").exists());
    }

    private void assertBReportFilesExist() {

        File reportDir = reportDir(projectDir().toPath().resolve("b").toFile());
        Assert.assertTrue(resolve(reportDir, "index.html").exists());
        Assert.assertTrue(resolve(reportDir, "org/hello/b/WorldB.scala.html").exists());
    }

    private void assertCommonReportFilesExist() {

        File reportDir = reportDir(projectDir().toPath().resolve("common").toFile());
        Assert.assertTrue(resolve(reportDir, "index.html").exists());
        Assert.assertTrue(resolve(reportDir, "org/hello/common/WorldCommon.scala.html").exists());
    }
}
