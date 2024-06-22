package org.scoverage;

import org.junit.Assert;

import java.util.List;

public class ScalaSingleModuleTestScala3 extends ScalaSingleModuleTest {

    @Override
    protected List<String> getVersionAgruments() {
        return ScalaVersionArguments.version3;
    }

    @Override
    public void checkScoverage() throws Exception {
        AssertableBuildResult result = run("clean", ScoveragePlugin.getCHECK_NAME());

        result.assertTaskSucceeded(ScoveragePlugin.getCOMPILE_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());

        assertReportFilesExist();
        assertCoverage(66.67);
    }

    @Override
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
    }

    @Override
    public void reportScoverageWithoutNormalCompilationAndWithExcludedClasses() throws Exception {
        AssertableBuildResult result = run("clean", ScoveragePlugin.getREPORT_NAME(),
                "-PexcludedFile=.*", "-P" + ScoveragePlugin.getSCOVERAGE_COMPILE_ONLY_PROPERTY());

        Assert.assertTrue(resolve(reportDir(), "index.html").exists());
        Assert.assertFalse(resolve(reportDir(), "org/hello/World.scala.html").exists());
        assertCoverage(100.0); // coverage is 100 since no classes are covered

        // compiled class should exist in the default classes directory, but not in scoverage
        Assert.assertTrue(resolve(buildDir(), "classes/scala/main/org/hello/World.class").exists());
    }
}
