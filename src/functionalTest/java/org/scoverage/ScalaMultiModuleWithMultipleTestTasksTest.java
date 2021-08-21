package org.scoverage;

import org.junit.Assert;
import org.junit.Test;


import java.io.File;


public class ScalaMultiModuleWithMultipleTestTasksTest extends ScoverageFunctionalTest {


    public ScalaMultiModuleWithMultipleTestTasksTest() {
        super("scala-multi-module-multiple-test-tasks");
    }

    @Test
    public void reportScoverage() {

        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.getREPORT_NAME());

        result.assertTaskExists(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("b:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("common:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("intTest");
        result.assertTaskExists("reportIntTestScoverage");
        result.assertTaskExists("a:intTest");
        result.assertTaskExists("b:intTest");
        result.assertTaskExists("common:intTest");
        result.assertTaskExists("a:reportIntTestScoverage");
        result.assertTaskExists("b:reportIntTestScoverage");
        result.assertTaskExists("common:reportIntTestScoverage");
        result.assertTaskExists(ScoveragePlugin.getREPORT_NAME());
    }

    @Test
    public void reportScoverageOnlyRoot() {

        AssertableBuildResult result = dryRun("clean", ":" + ScoveragePlugin.getREPORT_NAME());

        result.assertTaskExists(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist("b:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist("common:" + ScoveragePlugin.getREPORT_NAME());
    }

    @Test
    public void reportScoverageOnlyA() {

        AssertableBuildResult result = dryRun("clean", ":a:" + ScoveragePlugin.getREPORT_NAME());

        result.assertTaskDoesntExist(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist("b:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist("common:" + ScoveragePlugin.getREPORT_NAME());
    }

    @Test
    public void aggregateScoverage() {

        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.getAGGREGATE_NAME());

        result.assertTaskExists(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("b:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists(ScoveragePlugin.getAGGREGATE_NAME());
        result.assertTaskExists("common:" + ScoveragePlugin.getREPORT_NAME());
    }

    @Test
    public void checkScoverage() {

        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.getCHECK_NAME());

        result.assertTaskExists(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("b:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("common:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskExists("a:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskExists("b:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskExists("common:" + ScoveragePlugin.getCHECK_NAME());
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
        result.assertTaskDoesntExist("common:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskExists("a:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskDoesntExist("b:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskDoesntExist("common:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());
    }

    @Test
    public void checkScoverageWithoutIntTests() {
        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.getCHECK_NAME(),
                "-x", "intTest");

        result.assertTaskExists(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("b:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("common:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist("intTest");
        result.assertTaskExists("reportIntTestScoverage");
        result.assertTaskDoesntExist("a:intTest");
        result.assertTaskDoesntExist("b:intTest");
        result.assertTaskDoesntExist("common:intTest");
        result.assertTaskExists("a:reportIntTestScoverage");
        result.assertTaskExists("b:reportIntTestScoverage");
        result.assertTaskExists("common:reportIntTestScoverage");
    }

    @Test
    public void checkAndAggregateScoverage() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.getCHECK_NAME(),
                ScoveragePlugin.getAGGREGATE_NAME());

        result.assertTaskSucceeded(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("b:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("common:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded("a:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded("b:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded("common:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getAGGREGATE_NAME());

        assertAllReportFilesExist();
        assertCoverage(100.0);
    }

    @Test
    public void checkScoverageWithoutCoverageInRoot() throws Exception {

        AssertableBuildResult result = runAndFail("clean", ScoveragePlugin.getCHECK_NAME(),
                "test",
                "--tests", "org.hello.TestNothingSuite",
                "--tests", "org.hello.a.WorldASuite",
                "--tests", "org.hello.b.WorldBSuite",
                "--tests", "org.hello.common.WorldCommonSuite",
                "-x", "intTest");

        result.assertTaskFailed(ScoveragePlugin.getCHECK_NAME());

        assertRootReportFilesExist();
        assertCoverage(0.0);
    }

    @Test
    public void checkScoverageWithoutCoverageInA() throws Exception {

        AssertableBuildResult result = runAndFail("clean", ScoveragePlugin.getCHECK_NAME(),
                "test",
                "--tests", "org.hello.a.TestNothingASuite",
                "--tests", "org.hello.WorldSuite",
                "--tests", "org.hello.b.WorldBSuite",
                "--tests", "org.hello.common.WorldCommonSuite",
                "-x", ":a:intTest");

        result.assertTaskFailed("a:" + ScoveragePlugin.getCHECK_NAME());

        assertAReportFilesExist();
        assertCoverage(0.0, reportDir(projectDir().toPath().resolve("a").toFile()));
    }

    @Test
    public void checkScoverageWithoutNormalCompilationAndWithoutCoverageInCommon() throws Exception {

        AssertableBuildResult result = runAndFail("clean",
                ":a:test",
                ":common:test", "--tests", "org.hello.common.TestNothingCommonSuite",
                "-P" + ScoveragePlugin.getSCOVERAGE_COMPILE_ONLY_PROPERTY(),
                ScoveragePlugin.getCHECK_NAME());

        result.assertTaskFailed("common:" + ScoveragePlugin.getCHECK_NAME());

        assertCommonReportFilesExist();
        assertCoverage(0.0, reportDir(projectDir().toPath().resolve("common").toFile()));
    }

    @Test
    public void checkAndAggregateScoverageWithoutCoverageInRoot() throws Exception {

        // should pass as the check on the root is for the aggregation (which covers > 50%)

        AssertableBuildResult result = run("clean", ScoveragePlugin.getCHECK_NAME(),
                ScoveragePlugin.getAGGREGATE_NAME(), "test",
                "--tests", "org.hello.TestNothingSuite",
                "--tests", "org.hello.a.WorldASuite",
                "--tests", "org.hello.b.WorldBSuite",
                "--tests", "org.hello.common.WorldCommonSuite");

        result.assertTaskSucceeded(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("b:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("common:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded("a:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded("b:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded("common:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getAGGREGATE_NAME());

        assertAllReportFilesExist();
        assertCoverage(93.33);
    }

    @Test
    public void checkAndAggregateScoverageWithoutCoverageInAll() throws Exception {

        AssertableBuildResult result = runAndFail("clean", ScoveragePlugin.getCHECK_NAME(),
                ScoveragePlugin.getAGGREGATE_NAME(), "test",
                "--tests", "org.hello.TestNothingSuite",
                "--tests", "org.hello.a.TestNothingASuite",
                "--tests", "org.hello.b.TestNothingBSuite",
                "--tests", "org.hello.common.TestNothingCommonSuite",
                "-x", "intTest");

        result.assertTaskSucceeded(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("b:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("common:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getAGGREGATE_NAME());
        result.assertTaskFailed(ScoveragePlugin.getCHECK_NAME());

        assertAllReportFilesExist();
        assertCoverage(0.0);
    }

    @Test
    public void aggregateScoverageWithoutNormalCompilation() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.getAGGREGATE_NAME(),
                "-P" + ScoveragePlugin.getSCOVERAGE_COMPILE_ONLY_PROPERTY());

        result.assertTaskSkipped("compileScala");
        result.assertTaskSkipped("a:compileScala");
        result.assertTaskSkipped("b:compileScala");
        result.assertTaskSkipped("common:compileScala");
        result.assertTaskSucceeded(ScoveragePlugin.getCOMPILE_NAME());
        result.assertTaskSucceeded("a:" + ScoveragePlugin.getCOMPILE_NAME());
        result.assertTaskSucceeded("b:" + ScoveragePlugin.getCOMPILE_NAME());
        result.assertTaskSucceeded("common:" + ScoveragePlugin.getCOMPILE_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("a:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("b:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("common:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getAGGREGATE_NAME());

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

        Assert.assertTrue(resolve(reportDir(), "a/src/main/scala/org/hello/a/WorldA.scala.html").exists());
        Assert.assertTrue(resolve(reportDir(), "b/src/main/scala/org/hello/b/WorldB.scala.html").exists());
        Assert.assertTrue(resolve(reportDir(), "common/src/main/scala/org/hello/common/WorldCommon.scala.html").exists());
    }

    private void assertRootReportFilesExist() {

        Assert.assertTrue(resolve(reportDir(), "index.html").exists());
        Assert.assertTrue(resolve(reportDir(), "src/main/scala/org/hello/World.scala.html").exists());
    }

    private void assertAReportFilesExist() {

        File reportDir = reportDir(projectDir().toPath().resolve("a").toFile());
        Assert.assertTrue(resolve(reportDir, "index.html").exists());
        Assert.assertTrue(resolve(reportDir, "src/main/scala/org/hello/a/WorldA.scala.html").exists());
    }

    private void assertBReportFilesExist() {

        File reportDir = reportDir(projectDir().toPath().resolve("b").toFile());
        Assert.assertTrue(resolve(reportDir, "index.html").exists());
        Assert.assertTrue(resolve(reportDir, "src/main/scala/org/hello/b/WorldB.scala.html").exists());
    }

    private void assertCommonReportFilesExist() {

        File reportDir = reportDir(projectDir().toPath().resolve("common").toFile());
        Assert.assertTrue(resolve(reportDir, "index.html").exists());
        Assert.assertTrue(resolve(reportDir, "src/main/scala/org/hello/common/WorldCommon.scala.html").exists());
    }
}
