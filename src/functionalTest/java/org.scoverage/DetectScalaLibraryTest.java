package org.scoverage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class DetectScalaLibraryTest extends ScoverageFunctionalTest {

    private static final String SCALA_VERSION = "2.12";
    private static final String SCALA_LIBRARY_PARAMETER = "-PdetectedScalaLibraryVersion=";

    private static final String EXPECTED_OUTPUT_A = "Detected scala library in compilation classpath";
    private static final String EXPECTED_OUTPUT_B = "Using scoverage scalac plugin version '" + SCALA_VERSION;

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
        testWithParameter(SCALA_LIBRARY_PARAMETER + SCALA_VERSION + ".0");
        testWithParameter(SCALA_LIBRARY_PARAMETER + SCALA_VERSION + ".+");
    }

    private void testWithParameter(String parameter) {
        AssertableBuildResult result = dryRun("clean", parameter, "--info");
        String output = result.getResult().getOutput();
        Assert.assertTrue(output.contains(EXPECTED_OUTPUT_A));
        Assert.assertTrue(output.contains(EXPECTED_OUTPUT_B));
    }

}