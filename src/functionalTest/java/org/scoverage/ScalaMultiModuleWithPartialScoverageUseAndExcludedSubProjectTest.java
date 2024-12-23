package org.scoverage;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class ScalaMultiModuleWithPartialScoverageUseAndExcludedSubProjectTest extends ScoverageFunctionalTest {

    public ScalaMultiModuleWithPartialScoverageUseAndExcludedSubProjectTest() {
        super("scala-multi-module-with-partial-scoverage-use-and-excluded-sub-project");
    }

    @Test
    public void reportScoverage() {

        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.getREPORT_NAME());

        assertFalse(result.getResult().getOutput().contains("Scala sub-project 'a' doesn't have Scoverage applied and will be ignored in parent project aggregation"));
        result.assertTaskExists(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("b:" + ScoveragePlugin.getREPORT_NAME());
    }

}
