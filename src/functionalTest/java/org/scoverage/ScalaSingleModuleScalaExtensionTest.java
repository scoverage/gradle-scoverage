package org.scoverage;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public abstract class ScalaSingleModuleScalaExtensionTest extends ScoverageFunctionalTest {

    public ScalaSingleModuleScalaExtensionTest() {
        super("scala-single-module-scala-extension");
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
        Assert.assertTrue(resolve(reportDir(), "org/hello/World.scala.html").exists());
    }
}
