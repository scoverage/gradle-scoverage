package org.scoverage;

import scala.collection.Seq;
import scala.collection.Set;
import scoverage.Coverage;
import scoverage.IOUtils;
import scoverage.Serializer;
import scoverage.report.CoberturaXmlWriter;
import scoverage.report.ScoverageHtmlWriter;

import java.io.File;
import java.util.Arrays;

/**
 * late binding of scoverage core libraries (without a dependency on groovy)
 */
public class ScoverageReport {

    public static void main(String... args) {
        File sourceDir = new File(args[0]);
        File dataDir = new File(args[1]);
        File reportDir = new File(args[2]);
        reportDir.mkdirs();

        File coverageFile = Serializer.coverageFile(dataDir);
        File[] array = IOUtils.findMeasurementFiles(dataDir);
        // TODO: patch scoverage core to use a consistent collection type?
        Seq<File> measurementFiles = scala.collection.JavaConversions.asScalaBuffer(Arrays.asList(array));

        Coverage coverage = Serializer.deserialize(coverageFile);

        Set<Object> measurements = IOUtils.invoked(measurementFiles);
        coverage.apply(measurements);

        new ScoverageHtmlWriter(sourceDir, reportDir).write(coverage);
        new CoberturaXmlWriter(sourceDir, reportDir).write(coverage);
    }
}