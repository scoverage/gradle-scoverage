package org.scoverage;

import org.junit.Assert;
import org.junit.Test;

public class ScalaSingleModuleWithDepdencyManagerTest extends ScoverageFunctionalTest {

    public ScalaSingleModuleWithDepdencyManagerTest() {
        super("scala-single-module-dependency-manager");
    }

    @Test
    public void checkScoverage() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.getCHECK_NAME());

        result.assertTaskSucceeded(ScoveragePlugin.getCOMPILE_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());

        assertReportFilesExist();
        assertCoverage(100.0);
    }

    private void assertReportFilesExist() {

        Assert.assertTrue(resolve(reportDir(), "index.html").exists());
        Assert.assertTrue(resolve(reportDir(), "src/main/scala/org/hello/World.scala.html").exists());
    }
}