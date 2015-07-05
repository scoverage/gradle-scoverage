package org.scoverage;

import scala.collection.Seq;
import scala.collection.Set;
import scoverage.Coverage;
import scoverage.IOUtils;
import scoverage.Serializer;

import java.io.File;
import java.util.Arrays;

/**
 * late binding of scoverage core libraries (without a dependency on groovy)
 */
public class SingleReportApp {

    public static void main(String... args) {
        File sourceDir = new File(args[0]);
        File dataDir = new File(args[1]);
        File reportDir = new File(args[2]);

        Boolean coverageOutputCobertura = java.lang.Boolean.valueOf(args[3]);
        Boolean coverageOutputXML = java.lang.Boolean.valueOf(args[4]);
        Boolean coverageOutputHTML = java.lang.Boolean.valueOf(args[5]);
        Boolean coverageDebug = java.lang.Boolean.valueOf(args[6]);

        File coverageFile = Serializer.coverageFile(dataDir);

        if (!coverageFile.exists()) {
            System.out.println("[scoverage] Could not find coverage file, skipping...");
        } else {
            File[] array = IOUtils.findMeasurementFiles(dataDir);
            // TODO: patch scoverage core to use a consistent collection type?
            Seq<File> measurementFiles = scala.collection.JavaConversions.asScalaBuffer(Arrays.asList(array));

            Coverage coverage = Serializer.deserialize(coverageFile);

            Set<Object> measurements = IOUtils.invoked(measurementFiles);
            coverage.apply(measurements);

            ScoverageWriter.write(
                sourceDir,
                reportDir,
                coverage,
                coverageOutputCobertura,
                coverageOutputXML,
                coverageOutputHTML,
                coverageDebug);
        }
    }


}