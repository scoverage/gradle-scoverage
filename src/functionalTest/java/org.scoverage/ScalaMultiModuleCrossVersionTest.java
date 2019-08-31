package org.scoverage;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class ScalaMultiModuleCrossVersionTest extends ScoverageFunctionalTest {

    public ScalaMultiModuleCrossVersionTest() {
        super("scala-multi-module-cross-version");
    }

    @Test
    public void checkAndAggregateScoverage() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.getCHECK_NAME(),
                ScoveragePlugin.getAGGREGATE_NAME());

        result.assertTaskSkipped(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("2_11:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("2_12:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("2_13:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded("2_11:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded("2_12:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded("2_13:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getAGGREGATE_NAME());

        assertAllReportFilesExist();
        assertCoverage(100.0);
    }

    private void assertAllReportFilesExist() {

        assert211ReportFilesExist();
        assert212ReportFilesExist();
        assert213ReportFilesExist();
        assertAggregationFilesExist();
    }

    private void assertAggregationFilesExist() {

        Assert.assertTrue(resolve(reportDir(), "index.html").exists());
        Assert.assertTrue(resolve(reportDir(), "2_11/src/main/scala/org/hello/World211.scala.html").exists());
        Assert.assertTrue(resolve(reportDir(), "2_12/src/main/scala/org/hello/World212.scala.html").exists());
        Assert.assertTrue(resolve(reportDir(), "2_13/src/main/scala/org/hello/World213.scala.html").exists());
    }

    private void assert211ReportFilesExist() {

        File reportDir = reportDir(projectDir().toPath().resolve("2_11").toFile());
        Assert.assertTrue(resolve(reportDir, "index.html").exists());
        Assert.assertTrue(resolve(reportDir, "src/main/scala/org/hello/World211.scala.html").exists());
    }

    private void assert212ReportFilesExist() {

        File reportDir = reportDir(projectDir().toPath().resolve("2_12").toFile());
        Assert.assertTrue(resolve(reportDir, "index.html").exists());
        Assert.assertTrue(resolve(reportDir, "src/main/scala/org/hello/World212.scala.html").exists());
    }

    private void assert213ReportFilesExist() {

        File reportDir = reportDir(projectDir().toPath().resolve("2_13").toFile());
        Assert.assertTrue(resolve(reportDir, "index.html").exists());
        Assert.assertTrue(resolve(reportDir, "src/main/scala/org/hello/World213.scala.html").exists());
    }
}
