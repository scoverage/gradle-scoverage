package org.scoverage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class DetectScalaLibraryTest extends ScoverageFunctionalTest {

    private static final String SCALA_VERSION = "0.0";
    private static final String SCALA_LIBRARY_PARAMETER = "-PdetectedScalaLibraryVersion=" + SCALA_VERSION + ".0";
    private static final String EXPECTED_OUTPUT = "Using scoverage scalac plugin version '" + SCALA_VERSION;

    @Parameterized.Parameter(0)
    public String projectDir;

    @Parameterized.Parameters(name = "{index}: Project {0} ")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{{"/compile"}, {"/compileOnly"}, {"/implementation"}, {"/dependency-management"}};
        return Arrays.asList(data);
    }

    public DetectScalaLibraryTest() {
        super(null);
    }

    @Test
    public void test() {
        setProjectName("detect-scala-library" + projectDir);

        // build supposed to fail since repositories are not configured
        AssertableBuildResult result = runAndFail("clean", SCALA_LIBRARY_PARAMETER, "--info");

        // all we want to know is that the plugin detected our configured library version
        String output = result.getResult().getOutput();
        Assert.assertTrue(output.contains(EXPECTED_OUTPUT));
    }

}