package org.scoverage;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

public class ScalaSingleModuleTest extends ScoverageFunctionalTest {

    public ScalaSingleModuleTest() {
        super("scala-single-module");
    }

    @Override
    protected List<String> getVersionAgruments() {
        return ScalaVersionArguments.version2;
    }

    @Test
    public void test() {

        AssertableBuildResult result = dryRun("clean", "test");

        result.assertTaskDoesntExist(ScoveragePlugin.getCOMPILE_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getCHECK_NAME());
    }

    @Test
    public void build() {

        AssertableBuildResult result = dryRun("clean", "build");

        result.assertTaskDoesntExist(ScoveragePlugin.getCOMPILE_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getCHECK_NAME());
    }

    @Test
    public void reportScoverage() {

        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.getREPORT_NAME());

        result.assertTaskExists(ScoveragePlugin.getCOMPILE_NAME());
        result.assertTaskExists(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getCHECK_NAME());
    }

    @Test
    public void aggregateScoverage() {

        AssertableBuildResult result = runAndFail("clean", ScoveragePlugin.getAGGREGATE_NAME());

        result.assertNoTasks();
    }

    @Test
    public void checkScoverage() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.getCHECK_NAME());

        result.assertTaskSucceeded(ScoveragePlugin.getCOMPILE_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());

        assertReportFilesExist();
        assertCoverage(50.0);
    }

    @Test
    public void checkScoverageFails() throws Exception {

        AssertableBuildResult result = runAndFail("clean", ScoveragePlugin.getCHECK_NAME(),
                "test", "--tests", "org.hello.TestNothingSuite");

        result.assertTaskSucceeded(ScoveragePlugin.getCOMPILE_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskFailed(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());

        assertReportFilesExist();
        assertCoverage(0.0);
    }

    @Test
    public void reportScoverageWithExcludedClasses() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.getREPORT_NAME(),
                "-PexcludedFile=.*");

        result.assertTaskSucceeded(ScoveragePlugin.getCOMPILE_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());

        Assert.assertTrue(resolve(reportDir(), "index.html").exists());
        Assert.assertFalse(resolve(reportDir(), "org/hello/World.scala.html").exists());
        assertCoverage(100.0); // coverage is 100 since no classes are covered

        // compiled class should exist in the default classes directory, but not in scoverage
        Assert.assertTrue(resolve(buildDir(), "classes/scala/main/org/hello/World.class").exists());
        Assert.assertFalse(resolve(buildDir(), "classes/scala/scoverage/org/hello/World.class").exists());
    }

    @Test
    @Ignore
    public void reportScoverageWithoutNormalCompilation() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.getREPORT_NAME(),
                "-P" + ScoveragePlugin.getSCOVERAGE_COMPILE_ONLY_PROPERTY());

        result.assertTaskSkipped("compileScala");
        result.assertTaskSucceeded(ScoveragePlugin.getCOMPILE_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());

        assertReportFilesExist();
        assertCoverage(50.0);

        Assert.assertTrue(resolve(buildDir(), "classes/scala/main/org/hello/World.class").exists());
        Assert.assertFalse(resolve(buildDir(), "classes/scala/scoverage/org/hello/World.class").exists());
    }

    @Test
    public void reportScoverageWithoutNormalCompilationAndWithExcludedClasses() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.getREPORT_NAME(),
                "-PexcludedFile=.*", "-P" + ScoveragePlugin.getSCOVERAGE_COMPILE_ONLY_PROPERTY());

        Assert.assertTrue(resolve(reportDir(), "index.html").exists());
        Assert.assertFalse(resolve(reportDir(), "org/hello/World.scala.html").exists());
        assertCoverage(100.0); // coverage is 100 since no classes are covered

        // compiled class should exist in the default classes directory, but not in scoverage
        Assert.assertTrue(resolve(buildDir(), "classes/scala/main/org/hello/World.class").exists());
        Assert.assertFalse(resolve(buildDir(), "classes/scala/scoverage/org/hello/World.class").exists());
    }

    protected void assertReportFilesExist() {

        Assert.assertTrue(resolve(reportDir(), "index.html").exists());
        Assert.assertTrue(resolve(reportDir(), "org/hello/World.scala.html").exists());
    }
}
