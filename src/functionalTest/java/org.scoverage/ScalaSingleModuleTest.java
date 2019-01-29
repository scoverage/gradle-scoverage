package org.scoverage;

import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Test;

public class ScalaSingleModuleTest extends ScoverageFunctionalTest {

    public ScalaSingleModuleTest() {
        super("scala-single-module");
    }

    @Test
    public void test() {

        AssertableBuildResult result = dryRun("clean", "test");

        result.assertTaskDoesntExist(ScoveragePlugin.getCOMPILE_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getTEST_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getCHECK_NAME());
    }

    @Test
    public void build() {

        AssertableBuildResult result = dryRun("clean", "build");

        result.assertTaskDoesntExist(ScoveragePlugin.getCOMPILE_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getTEST_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getCHECK_NAME());
    }

    @Test
    public void testScoverage() {

        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.getTEST_NAME());

        result.assertTaskExists(ScoveragePlugin.getCOMPILE_NAME());
        result.assertTaskExists(ScoveragePlugin.getTEST_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getCHECK_NAME());
    }

    @Test
    public void reportScoverage() {

        AssertableBuildResult result = dryRun("clean", ScoveragePlugin.getREPORT_NAME());

        result.assertTaskExists(ScoveragePlugin.getCOMPILE_NAME());
        result.assertTaskExists(ScoveragePlugin.getTEST_NAME());
        result.assertTaskExists(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());
        result.assertTaskDoesntExist(ScoveragePlugin.getCHECK_NAME());
    }

    @Test
    public void aggregateScoverage() {

        AssertableBuildResult result = runAndFail("clean", ScoveragePlugin.getAGGREGATE_NAME());

        result.assertNoTasks();
    }

    @Test
    public void checkScoverage() {

        AssertableBuildResult result = run("clean", ScoveragePlugin.getCHECK_NAME());

        result.assertTaskOutcome(ScoveragePlugin.getCOMPILE_NAME(), TaskOutcome.SUCCESS);
        result.assertTaskOutcome(ScoveragePlugin.getTEST_NAME(), TaskOutcome.SUCCESS);
        result.assertTaskOutcome(ScoveragePlugin.getREPORT_NAME(), TaskOutcome.SUCCESS);
        result.assertTaskOutcome(ScoveragePlugin.getCHECK_NAME(), TaskOutcome.SUCCESS);
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());
    }

    @Test
    public void checkScoverageFails() {

        AssertableBuildResult result = runAndFail("clean", ScoveragePlugin.getCHECK_NAME(),
                ScoveragePlugin.getTEST_NAME(), "--tests", "org.hello.TestNothingSuite");

        result.assertTaskOutcome(ScoveragePlugin.getCOMPILE_NAME(), TaskOutcome.SUCCESS);
        result.assertTaskOutcome(ScoveragePlugin.getTEST_NAME(), TaskOutcome.SUCCESS);
        result.assertTaskOutcome(ScoveragePlugin.getREPORT_NAME(), TaskOutcome.SUCCESS);
        result.assertTaskOutcome(ScoveragePlugin.getCHECK_NAME(), TaskOutcome.FAILED);
        result.assertTaskDoesntExist(ScoveragePlugin.getAGGREGATE_NAME());
    }
}