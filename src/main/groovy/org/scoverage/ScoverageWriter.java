package org.scoverage;

import scoverage.Constants;
import scoverage.Coverage;
import scoverage.report.CoberturaXmlWriter;
import scoverage.report.ScoverageHtmlWriter;
import scoverage.report.ScoverageXmlWriter;

import java.io.File;

/**
 * Util for generating and saving coverage files.
 * <p/>
 * Copied from sbt-scoverage and converted to Java to avoid dependency to Scala.
 */
public class ScoverageWriter {

    /**
     * Generates all reports from given data.
     *
     * @param sourceDir               directory with project sources
     * @param reportDir               directory for generate reports
     * @param coverage                coverage data
     * @param coverageOutputCobertura switch for Cobertura output
     * @param coverageOutputXML       switch for Scoverage XML output
     * @param coverageOutputHTML      switch for Scoverage HTML output
     * @param coverageDebug           switch for Scoverage Debug output
     */
    public static void write(File sourceDir,
                             File reportDir,
                             Coverage coverage,
                             Boolean coverageOutputCobertura,
                             Boolean coverageOutputXML,
                             Boolean coverageOutputHTML,
                             Boolean coverageDebug) {

        System.out.println("[scoverage] Generating scoverage reports...");

        reportDir.mkdirs();

        if (coverageOutputCobertura) {
            new CoberturaXmlWriter(sourceDir, reportDir).write(coverage);
            System.out.println("[scoverage] Written Cobertura XML report to " +
                reportDir.getAbsolutePath() +
                File.separator +
                "cobertura.xml");
        }

        if (coverageOutputXML) {
            new ScoverageXmlWriter(sourceDir, reportDir, /* debug = */ false).write(coverage);
            System.out.println("[scoverage] Written XML report to " +
                reportDir.getAbsolutePath() +
                File.separator +
                Constants.XMLReportFilename());
            if (coverageDebug) {
                new ScoverageXmlWriter(sourceDir, reportDir, /* debug = */ true).write(coverage);
                System.out.println("[scoverage] Written XML report with debug information to " +
                    reportDir.getAbsolutePath() +
                    File.separator +
                    Constants.XMLReportFilenameWithDebug());
            }
        }

        if (coverageOutputHTML) {
            new ScoverageHtmlWriter(sourceDir, reportDir).write(coverage);
            System.out.println("[scoverage] Written HTML report to " +
                reportDir.getAbsolutePath() +
                File.separator +
                "index.html");
        }

        System.out.println("[scoverage] Coverage reports completed");
    }
}
