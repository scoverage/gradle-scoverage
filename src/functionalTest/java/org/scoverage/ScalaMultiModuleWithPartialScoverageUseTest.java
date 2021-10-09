package org.scoverage;

import org.junit.Test;

public class ScalaMultiModuleWithPartialScoverageUseTest extends ScoverageFunctionalTest {

    public ScalaMultiModuleWithPartialScoverageUseTest() {
        super("scala-multi-module-with-partial-scoverage-use");
    }

    @Test
    public void reportScoverage() {

        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.REPORT_NAME);

        result.assertTaskExists(ScoveragePlugin.REPORT_NAME);
        result.assertTaskExists("b:" + ScoveragePlugin.REPORT_NAME);
    }

}
