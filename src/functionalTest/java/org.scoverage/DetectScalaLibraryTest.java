package org.scoverage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@RunWith(Parameterized.class)
public class DetectScalaLibraryTest extends ScoverageFunctionalTest {

    private static final String SCALA_VERSION = "2.12";
    private static final String SCALA_LIBRARY_PARAMETER = "-PdetectedScalaLibraryVersion=";

    private static final String EXPECTED_OUTPUT_A = "Detected scala library in compilation classpath";
    private static final String EXPECTED_OUTPUT_B = "Using scoverage scalac plugin version '" + SCALA_VERSION;

    @Parameterized.Parameter(0)
    public String projectDir;

    @Parameterized.Parameter(1)
    public String[] subVersions;

    @Parameterized.Parameter(2)
    public String[] additionalParameters;

    @Parameterized.Parameters(name = "{index}: Project {0} ")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {"/compile", new String[] {".0", ".+"}, new String[0]},
                {"/compileOnly", new String[] {".0", ".+"}, new String[0]},
                {"/implementation", new String[] {".0", ".+"}, new String[0]},
                {"/dependency-management", new String[] {".0", ".+"}, new String[0]},
                {"/gradle-consistent-versions", new String[] {"ignored"}, new String[] {"--write-locks"}},
        };
        return Arrays.asList(data);
    }

    public DetectScalaLibraryTest() {
        super(null);
    }

    @Test
    public void test() {
        setProjectName("detect-scala-library" + projectDir);
        for (String subVersion : subVersions) {
            testWithParameter(SCALA_LIBRARY_PARAMETER + SCALA_VERSION + subVersion);
        }
    }

    private void testWithParameter(String parameter) {

        String[] basicParameters = {"clean", parameter, "--info"};
        String[] parameters = Stream.concat(Arrays.stream(basicParameters), Arrays.stream(additionalParameters))
                .toArray(String[]::new);
        AssertableBuildResult result = dryRun(parameters);

        String output = result.getResult().getOutput();
        Assert.assertTrue(output.contains(EXPECTED_OUTPUT_A));
        Assert.assertTrue(output.contains(EXPECTED_OUTPUT_B));
    }

}