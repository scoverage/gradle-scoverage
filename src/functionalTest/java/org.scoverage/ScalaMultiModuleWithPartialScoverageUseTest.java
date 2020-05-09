package org.scoverage;

import org.junit.Test;

public class ScalaMultiModuleWithPartialScoverageUseTest extends ScoverageFunctionalTest {

    public ScalaMultiModuleWithPartialScoverageUseTest() {
        super("scala-multi-module-with-partial-scoverage-use");
    }

    @Test
    public void reportScoverage() {

        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.getREPORT_NAME());

        result.assertTaskExists(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskExists("b:" + ScoveragePlugin.getREPORT_NAME());
    }

}
