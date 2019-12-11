package org.scoverage;

import org.gradle.api.logging.Logger;
import scala.Some;
import scala.collection.JavaConverters;
import scala.collection.mutable.Buffer;
import scoverage.Constants;
import scoverage.Coverage;
import scoverage.report.CoberturaXmlWriter;
import scoverage.report.ScoverageHtmlWriter;
import scoverage.report.ScoverageXmlWriter;

import java.io.File;
import java.util.Arrays;

/**
 * Util for generating and saving coverage files.
 * <p/>
 * Copied from sbt-scoverage and converted to Java to avoid dependency to Scala.
 */
public class ScoverageWriter {

    private final Logger logger;

    public ScoverageWriter(Logger logger) {

        this.logger = logger;
    }

    /**
     * Generates all reports from given data.
     *
     * @param sourceDir               directory with project sources
     * @param reportDir               directory for generate reports
     * @param coverage                coverage data
     * @param sourceEncoding          the encoding of the source files
     * @param coverageOutputCobertura switch for Cobertura output
     * @param coverageOutputXML       switch for Scoverage XML output
     * @param coverageOutputHTML      switch for Scoverage HTML output
     * @param coverageDebug           switch for Scoverage Debug output
     */
    public void write(File sourceDir,
                             File reportDir,
                             Coverage coverage,
                             String sourceEncoding,
                             Boolean coverageOutputCobertura,
                             Boolean coverageOutputXML,
                             Boolean coverageOutputHTML,
                             Boolean coverageDebug) {

        logger.info("[scoverage] Generating scoverage reports...");

        reportDir.mkdirs();

        if (coverageOutputCobertura) {
            new CoberturaXmlWriter(sourceDir, reportDir).write(coverage);
            logger.info("[scoverage] Written Cobertura XML report to " +
                reportDir.getAbsolutePath() +
                File.separator +
                "cobertura.xml");
        }

        if (coverageOutputXML) {
            new ScoverageXmlWriter(sourceDir, reportDir, /* debug = */ false).write(coverage);
            logger.info("[scoverage] Written XML report to " +
                reportDir.getAbsolutePath() +
                File.separator +
                Constants.XMLReportFilename());
            if (coverageDebug) {
                new ScoverageXmlWriter(sourceDir, reportDir, /* debug = */ true).write(coverage);
                logger.info("[scoverage] Written XML report with debug information to " +
                    reportDir.getAbsolutePath() +
                    File.separator +
                    Constants.XMLReportFilenameWithDebug());
            }
        }

        if (coverageOutputHTML) {
            Buffer<File> sources = JavaConverters.asScalaBuffer(Arrays.asList(sourceDir));
            new ScoverageHtmlWriter(sources, reportDir, new Some<>(sourceEncoding)).write(coverage);
            logger.info("[scoverage] Written HTML report to " +
                reportDir.getAbsolutePath() +
                File.separator +
                "index.html");
        }

        logger.info("[scoverage] Coverage reports completed");
    }
}
