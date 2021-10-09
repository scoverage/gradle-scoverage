package org.scoverage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;

@RunWith(Parameterized.class)
public class DetectScalaLibraryTest extends ScoverageFunctionalTest {

    private static final String SCALA_VERSION = "2.13";
    private static final String SCALA_LIBRARY_PARAMETER = "-PdetectedScalaLibraryVersion=";

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
                {"/compile", new String[]{".0", ".+"}, true, new String[0]},
                {"/compileOnly", new String[]{".0", ".+"}, true, new String[0]},
                {"/implementation", new String[]{".0", ".+"}, true, new String[0]},
                {"/dependency-management", new String[]{".0", ".+"}, true, new String[0]},
// disabled until the consistent-versions plugin supports Gradle 7
//                {"/gradle-consistent-versions", new String[] {"ignored"}, false, new String[] {"--write-locks"}},
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

    private void testWithParameter(String parameter, boolean detect) {

        String[] basicParameters = {"clean", parameter, "--info"};
        String[] parameters = Stream.concat(Arrays.stream(basicParameters), Arrays.stream(additionalParameters))
                .toArray(String[]::new);
        AssertableBuildResult result = dryRun(parameters);

        String output = result.getResult().getOutput();
        if (detect) {
            assertThat(output, containsString("Detected scala library in compilation classpath"));
        } else {
            assertThat(output, containsString("Using configured Scala version"));
        }
        assertThat(output, stringContainsInOrder("Using scoverage scalac plugin", "for scala", SCALA_VERSION));
    }

}
