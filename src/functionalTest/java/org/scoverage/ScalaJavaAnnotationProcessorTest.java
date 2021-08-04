package org.scoverage;

import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class ScalaJavaAnnotationProcessorTest extends ScoverageFunctionalTest {

    public ScalaJavaAnnotationProcessorTest() {
        super("scala-java-annotation-processor");
    }

    @Test
    public void checkAndAggregateScoverage() throws Exception {

        AssertableBuildResult result = run("clean", ScoveragePlugin.getCHECK_NAME(),
                ScoveragePlugin.getAGGREGATE_NAME());

        result.assertTaskSkipped("java_only:" + ScoveragePlugin.getCOMPILE_NAME());

        result.assertTaskSkipped(ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSucceeded("mixed_scala_java:" + ScoveragePlugin.getREPORT_NAME());
        result.assertTaskSkipped("java_only:" + ScoveragePlugin.getREPORT_NAME());

        result.assertTaskSucceeded(ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSucceeded("mixed_scala_java:" + ScoveragePlugin.getCHECK_NAME());
        result.assertTaskSkipped("java_only:" + ScoveragePlugin.getCHECK_NAME());

        result.assertTaskSucceeded(ScoveragePlugin.getAGGREGATE_NAME());

        assertAllReportFilesExist();
        assertCoverage(100.0);
    }

    private void assertAllReportFilesExist() {

        Assert.assertTrue(resolve(reportDir(), "index.html").exists());

        assertMixedScalaJavaReportFilesExist();
        assertAggregationFilesExist();
    }

    private void assertAggregationFilesExist() {

        Assert.assertTrue(resolve(reportDir(), "mixed_scala_java/src/main/scala/org/hello/WorldScala.scala.html").exists());
    }

    private void assertMixedScalaJavaReportFilesExist() {

        File reportDir = reportDir(projectDir().toPath().resolve("mixed_scala_java").toFile());
        Assert.assertTrue(resolve(reportDir, "index.html").exists());
        Assert.assertTrue(resolve(reportDir, "src/main/scala/org/hello/WorldScala.scala.html").exists());
    }
}
