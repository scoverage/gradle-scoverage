package org.scoverage;

import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests are currently ignored as composite builds are not supported yet.
 *
 * @see <a href="https://github.com/scoverage/gradle-scoverage/issues/98">Issue #94</a>.
 */
public class CompositeBuildTest extends ScoverageFunctionalTest {

    public CompositeBuildTest() {
        super("composite-build");
    }

    @Ignore
    @Test
    public void buildComposite() {

        runComposite("clean", "build");
    }

    @Ignore
    @Test
    public void reportComposite() {

        runComposite("clean", ScoveragePlugin.REPORT_NAME);
    }

    private AssertableBuildResult runComposite(String... arguments) {

        List<String> fullArguments = new ArrayList<>();
        fullArguments.add("-p");
        fullArguments.add("proj1");
        fullArguments.add("--include-build");
        fullArguments.add("../proj2");
        fullArguments.addAll(Arrays.asList(arguments));

        return run(fullArguments.toArray(new String[0]));
    }
}
