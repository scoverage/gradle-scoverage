package org.scoverage;

import org.junit.Assert;
import org.junit.Test;

public abstract class MultipleCheckTasksTest extends ScoverageFunctionalTest {

    /* --- Abstract Test ---- */

    private final boolean shouldSucceed;

    public MultipleCheckTasksTest(String projectDir, boolean shouldSucceed) {
        super("multiple-check-tasks/" + projectDir);
        this.shouldSucceed = shouldSucceed;
    }

    @Test
    public void test() {
        assertResult(run());
    }

    protected abstract void assertResult(AssertableBuildResult result);

    protected void assertOutput(AssertableBuildResult result, CoverageType type, double minimumRate) {
        String expectedMessage = String.format("Checking coverage. Type: %s. Minimum rate: %s", type, minimumRate);
        Assert.assertTrue(result.getResult().getOutput().contains(expectedMessage));
    }

    private AssertableBuildResult run() {
        if (shouldSucceed) {
            return run("clean", ScoveragePlugin.CHECK_NAME, "--info");
        } else {
            return runAndFail(ScoveragePlugin.CHECK_NAME, "--info");
        }
    }

    /* --- Test Classes ---- */

    public static class MultipleChecks extends MultipleCheckTasksTest {
        public MultipleChecks() {
            super("multiple-checks", true);
        }

        @Override
        protected void assertResult(AssertableBuildResult result) {

            result.assertTaskSucceeded(ScoveragePlugin.CHECK_NAME);
            assertOutput(result, CoverageType.Line, 0.3);
            assertOutput(result, CoverageType.Branch, 0.1);
            assertOutput(result, CoverageType.Statement, 0.6);

        }
    }

    public static class SingleCheckNewSyntax extends MultipleCheckTasksTest {
        public SingleCheckNewSyntax() {
            super("single-check-new-syntax", true);
        }

        @Override
        protected void assertResult(AssertableBuildResult result) {
            result.assertTaskSucceeded(ScoveragePlugin.CHECK_NAME);
            assertOutput(result, CoverageType.Line, 0.3);
        }
    }

    public static class SingleCheckOldSyntax extends MultipleCheckTasksTest {
        public SingleCheckOldSyntax() {
            super("single-check-old-syntax", true);
        }

        @Override
        protected void assertResult(AssertableBuildResult result) {
            result.assertTaskSucceeded(ScoveragePlugin.CHECK_NAME);
            assertOutput(result, CoverageType.Line, 0.3);
        }
    }

    public static class OldAndNewSyntax extends MultipleCheckTasksTest {
        public OldAndNewSyntax() {
            super("old-and-new-syntax", false);
        }

        @Override
        protected void assertResult(AssertableBuildResult result) {
        }
    }

    public static class NoCheck extends MultipleCheckTasksTest {
        public NoCheck() {
            super("no-check", true);
        }

        @Override
        protected void assertResult(AssertableBuildResult result) {
            result.assertTaskSucceeded(ScoveragePlugin.CHECK_NAME);
            assertOutput(result, ScoverageExtension.DEFAULT_COVERAGE_TYPE, ScoverageExtension.DEFAULT_MINIMUM_RATE);
        }
    }
}
