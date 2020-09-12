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

    private static final String EXPECTED_OUTPUT_CONFIGURED_VERSION = "Using configured Scala version";
    private static final String EXPECTED_OUTPUT_DETECTED_VERSION = "Detected scala library in compilation classpath";
    private static final String EXPECTED_OUTPUT_USING = "Using scoverage scalac plugin version '" + SCALA_VERSION;

    @Parameterized.Parameter(0)
    public String projectDir;

    @Parameterized.Parameter(1)
    public String[] subVersions;

    @Parameterized.Parameter(2)
    public boolean detect;

    @Parameterized.Parameter(3)
    public String[] additionalParameters;

    @Parameterized.Parameters(name = "{index}: Project {0} ")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {"/compile", new String[] {".0", ".+"}, true, new String[0]},
                {"/compileOnly", new String[] {".0", ".+"}, true, new String[0]},
                {"/implementation", new String[] {".0", ".+"}, true, new String[0]},
                {"/dependency-management", new String[] {".0", ".+"}, true, new String[0]},
                {"/gradle-consistent-versions", new String[] {"ignored"}, false, new String[] {"--write-locks"}},
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
            testWithParameter(SCALA_LIBRARY_PARAMETER + SCALA_VERSION + subVersion, detect);
        }
    }

    private void testWithParameter(String parameter, Boolean detect) {

        String[] basicParameters = {"clean", parameter, "--info"};
        String[] parameters = Stream.concat(Arrays.stream(basicParameters), Arrays.stream(additionalParameters))
                .toArray(String[]::new);
        AssertableBuildResult result = dryRun(parameters);

        String output = result.getResult().getOutput();
        if (detect) {
            Assert.assertTrue(output.contains(EXPECTED_OUTPUT_DETECTED_VERSION));
        } else {
            Assert.assertTrue(output.contains(EXPECTED_OUTPUT_CONFIGURED_VERSION));
        }
        Assert.assertTrue(output.contains(EXPECTED_OUTPUT_USING));
    }

}