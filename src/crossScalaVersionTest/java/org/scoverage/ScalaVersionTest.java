package org.scoverage;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * This abstract class is used to test each scala version in an individual class.
 * It is crucial that each test will be separated into its own class,
 * as this is the only way to run these tests in separate JVM processes (via `forkEvery` gradle configuration).
 */
public abstract class ScalaVersionTest extends ScoverageFunctionalTest {

    private final String scalaVersion;

    public ScalaVersionTest(String scalaVersion) {
        super("scala-multi-module-cross-version");
        this.scalaVersion = scalaVersion;
    }

    @Test
    public void report() throws Exception {

        AssertableBuildResult result = run("clean", ":" + scalaVersion + ":" + ScoveragePlugin.REPORT_NAME);
        result.assertTaskSucceeded(scalaVersion + ":" + ScoveragePlugin.REPORT_NAME);

        File reportDir = reportDir(projectDir().toPath().resolve(scalaVersion).toFile());
        Assert.assertTrue(resolve(reportDir, "index.html").exists());
        Assert.assertTrue(resolve(reportDir, "org/hello/World" + scalaVersion + ".scala.html").exists());
    }
}
