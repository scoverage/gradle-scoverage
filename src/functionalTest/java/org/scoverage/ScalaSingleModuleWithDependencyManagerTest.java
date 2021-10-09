package org.scoverage;

import org.junit.Assert;
import org.junit.Test;

public class ScalaSingleModuleWithDependencyManagerTest extends ScoverageFunctionalTest {

    public ScalaSingleModuleWithDependencyManagerTest() {
        super("scala-single-module-dependency-manager");
    }

    @Test
    public void checkScoverage() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.CHECK_NAME);

        result.assertTaskSucceeded(ScoveragePlugin.COMPILE_NAME);
        result.assertTaskSucceeded(ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded(ScoveragePlugin.CHECK_NAME);
        result.assertTaskDoesntExist(ScoveragePlugin.AGGREGATE_NAME);

        assertReportFilesExist();
        assertCoverage(100.0);
    }

    private void assertReportFilesExist() {

        Assert.assertTrue(resolve(reportDir(), "index.html").exists());
        Assert.assertTrue(resolve(reportDir(), "org/hello/World.scala.html").exists());
    }
}
