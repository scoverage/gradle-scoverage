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

        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.getREPORT_NAME());

        result.assertTaskExists(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("b:" + ScoveragePlugin.getREPORT_NAME());
    }

    @Test
    public void reportScoverageOnlyRoot() {

        AssertableBuildResult result = dryRun("clean", ":" + ScoveragePlugin.getREPORT_NAME());

        result.assertTaskExists(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist("b:" + ScoveragePlugin.getREPORT_NAME());
    }

    @Test
    public void reportScoverageOnlyA() {

        AssertableBuildResult result = dryRun("clean", ":a:" + ScoveragePlugin.getREPORT_NAME());

        result.assertTaskDoesntExist(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist("b:" + ScoveragePlugin.getREPORT_NAME());
    }

    @Test
    public void aggregateScoverage() {

        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.getAGGREGATE_NAME());

        result.assertTaskExists(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("b:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists(ScoveragePlugin.getAGGREGATE_NAME());
    }

    @Test
    public void checkScoverage() {

        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.getCHECK_NAME());

        result.assertTaskExists(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("b:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskExists("a:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskExists("b:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());
    }

    @Test
    public void checkScoverageOnlyRoot() {

        AssertableBuildResult result = dryRun("clean", ":" + ScoveragePlugin.getCHECK_NAME());

        result.assertTaskExists(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist("b:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskDoesntExist("a:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskDoesntExist("b:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());
    }

    @Test
    public void checkScoverageOnlyA() {

        AssertableBuildResult result = dryRun("clean", ":a:" + ScoveragePlugin.getCHECK_NAME());

        result.assertTaskDoesntExist(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist("b:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskExists("a:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskDoesntExist("b:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());
    }

    @Test
    public void checkAndAggregateScoverage() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.getCHECK_NAME(),
                ScoveragePlugin.getAGGREGATE_NAME());

        result.assertTaskSucceeded(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("b:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded("a:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded("b:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getAGGREGATE_NAME());

        assertAllReportFilesExist();
        assertCoverage(100.0);
    }

    @Test
    public void checkScoverageWithoutCoverageInRoot() throws Exception {

        AssertableBuildResult result = runAndFail("clean", ScoveragePlugin.getCHECK_NAME(),
                ScoveragePlugin.getTEST_NAME(),
                "--tests", "org.hello.TestNothingSuite",
                "--tests", "org.hello.a.WorldASuite",
                "--tests", "org.hello.b.WorldBSuite");

        result.assertTaskFailed(ScoveragePlugin.getCHECK_NAME());

        assertRootReportFilesExist();
        assertCoverage(0.0);
    }

    @Test
    public void checkScoverageWithoutCoverageInA() throws Exception {

        AssertableBuildResult result = runAndFail("clean", ScoveragePlugin.getCHECK_NAME(),
                ScoveragePlugin.getTEST_NAME(),
                "--tests", "org.hello.a.TestNothingASuite",
                "--tests", "org.hello.WorldSuite",
                "--tests", "org.hello.b.WorldBSuite");

        result.assertTaskFailed("a:" + ScoveragePlugin.getCHECK_NAME());

        assertAReportFilesExist();
        assertCoverage(0.0, reportDir(projectDir().toPath().resolve("a").toFile()));
    }

    @Test
    public void checkAndAggregateScoverageWithoutCoverageInRoot() throws Exception {

        // should pass as the check on the root is for the aggregation (which covers > 50%)

        AssertableBuildResult result = run("clean", ScoveragePlugin.getCHECK_NAME(),
                ScoveragePlugin.getAGGREGATE_NAME(), ScoveragePlugin.getTEST_NAME(),
                "--tests", "org.hello.TestNothingSuite",
                "--tests", "org.hello.a.WorldASuite",
                "--tests", "org.hello.b.WorldBSuite");

        result.assertTaskSucceeded(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("b:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded("a:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded("b:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getAGGREGATE_NAME());

        assertAllReportFilesExist();
        assertCoverage(66.6);
    }

    @Test
    public void checkAndAggregateScoverageWithoutCoverageInAll() throws Exception {

        AssertableBuildResult result = runAndFail("clean", ScoveragePlugin.getCHECK_NAME(),
                ScoveragePlugin.getAGGREGATE_NAME(), ScoveragePlugin.getTEST_NAME(),
                "--tests", "org.hello.TestNothingSuite",
                "--tests", "org.hello.a.TestNothingASuite",
                "--tests", "org.hello.b.TestNothingBSuite");

        result.assertTaskSucceeded(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("b:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getAGGREGATE_NAME());
        result.assertTaskFailed(ScoveragePlugin.getCHECK_NAME());

        assertAllReportFilesExist();
        assertCoverage(0.0);
    }

    private void assertAllReportFilesExist() {

        assertRootReportFilesExist();
        assertAReportFilesExist();
        assertBReportFilesExist();
        assertAggregationFilesExist();
    }

    private void assertAggregationFilesExist() {

        assertRootReportFilesExist();
        Assert.assertTrue(resolve(reportDir(), "a/src/main/scala/org/hello/a/WorldA.scala.html").exists());
        Assert.assertTrue(resolve(reportDir(), "b/src/main/scala/org/hello/b/WorldB.scala.html").exists());
    }

    private void assertRootReportFilesExist() {

        Assert.assertTrue(resolve(reportDir(), "index.html").exists());
        Assert.assertTrue(resolve(reportDir(), "src/main/scala/org/hello/World.scala.html").exists());
    }

    private void assertAReportFilesExist() {

        File aReportDir = reportDir(projectDir().toPath().resolve("a").toFile());
        Assert.assertTrue(resolve(aReportDir, "index.html").exists());
        Assert.assertTrue(resolve(aReportDir, "src/main/scala/org/hello/a/WorldA.scala.html").exists());
    }

    private void assertBReportFilesExist() {

        File bReportDir = reportDir(projectDir().toPath().resolve("b").toFile());
        Assert.assertTrue(resolve(bReportDir, "index.html").exists());
        Assert.assertTrue(resolve(bReportDir, "src/main/scala/org/hello/b/WorldB.scala.html").exists());

    }
}
