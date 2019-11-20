package org.scoverage;

import org.junit.Assert;
import org.junit.Test;

public class MultiModulePluginNotConfiguredForScalaTest extends ScoverageFunctionalTest {

    public MultiModulePluginNotConfiguredForScalaTest() {
        super("multi-module-plugin-not-configured-for-scala");
    }

    @Test
    public void checkAndAggregateScoverage() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.getCHECK_NAME(),
                ScoveragePlugin.getAGGREGATE_NAME());

        result.assertTaskSkipped(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSkipped("scala_only:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSkipped("java_only:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSkipped(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSkipped("scala_only:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSkipped("java_only:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSkipped(ScoveragePlugin.getAGGREGATE_NAME());

        assertReportDirsEmpty();

        Assert.assertTrue(result.getResult().getOutput().contains("Scala sub-project 'scala_only' doesn't have Scoverage applied"));
        Assert.assertFalse(result.getResult().getOutput().contains("Scala sub-project 'java_only' doesn't have Scoverage applied"));
    }

    private void assertReportDirsEmpty() {

        Assert.assertFalse(reportDir().exists());
        Assert.assertFalse(reportDir(projectDir().toPath().resolve("scala_only").toFile()).exists());
        Assert.assertFalse(reportDir(projectDir().toPath().resolve("java_only").toFile()).exists());
    }
}
