package org.scoverage;

import org.junit.Assert;
import org.junit.Test;

public class ScalaCrossVersionAggregationTest extends ScoverageFunctionalTest {

    public ScalaCrossVersionAggregationTest() {
        super("scala-multi-module-cross-version");
    }

    @Test
    public void checkAndAggregateAll() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.getCHECK_NAME(),
                ScoveragePlugin.getAGGREGATE_NAME());

        result.assertTaskSkipped(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("2_12:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("2_13:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded("2_12:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded("2_13:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded(ScoveragePlugin.getAGGREGATE_NAME());

        assertAggregationFilesExist();
        assertCoverage(100.0);
    }

    private void assertAggregationFilesExist() {

        Assert.assertTrue(resolve(reportDir(), "index.html").exists());
        Assert.assertTrue(resolve(reportDir(), "org/hello/World2_12.scala.html").exists());
        Assert.assertTrue(resolve(reportDir(), "org/hello/World2_13.scala.html").exists());
    }
}
