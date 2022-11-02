package org.scoverage;

import org.gradle.api.logging.Logger;
import scala.Option;
import scala.Some;
import scala.collection.immutable.Seq;
import scala.collection.mutable.Buffer;
import scoverage.domain.Constants;
import scoverage.domain.Coverage;
import scoverage.reporter.CoberturaXmlWriter;
import scoverage.reporter.ScoverageHtmlWriter;
import scoverage.reporter.ScoverageXmlWriter;
import scala.collection.JavaConverters;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

/**
 * Util for generating and saving coverage files.
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
     * @param sourceDirs               directories with project sources
     * @param reportDir               directory for generate reports
     * @param coverage                coverage data
     * @param sourceEncoding          the encoding of the source files
     * @param coverageOutputCobertura switch for Cobertura output
     * @param coverageOutputXML       switch for Scoverage XML output
     * @param coverageOutputHTML      switch for Scoverage HTML output
     * @param coverageDebug           switch for Scoverage Debug output
     */
    public void write(Set<File> sourceDirs,
                             File reportDir,
                             Coverage coverage,
                             String sourceEncoding,
                             Boolean coverageOutputCobertura,
                             Boolean coverageOutputXML,
                             Boolean coverageOutputHTML,
                             Boolean coverageDebug) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {

        logger.info("[scoverage] Generating scoverage reports...");

        reportDir.mkdirs();

        Object scalaBuffer = Class.forName("scala.collection.JavaConverters")
                .getMethod("asScalaBuffer", java.util.List.class)
                .invoke(null, new ArrayList<>(sourceDirs));
        Object sourceDirsSeq = scalaBuffer.getClass().getMethod("toIndexedSeq").invoke(scalaBuffer);

        if (coverageOutputCobertura) {
            Constructor<CoberturaXmlWriter> cst;
            try {
                cst = CoberturaXmlWriter.class.getConstructor(
                        Class.forName("scala.collection.immutable.Seq"),
                        File.class,
                        Class.forName("scala.Option"));
            } catch (NoSuchMethodException | ClassNotFoundException e) {
                cst = CoberturaXmlWriter.class.getConstructor(
                        Class.forName("scala.collection.Seq"),
                        File.class,
                        Class.forName("scala.Option"));
            }
            CoberturaXmlWriter writer = cst.newInstance(sourceDirsSeq, reportDir, new Some<>(sourceEncoding));
            writer.write(coverage);
            logger.info("[scoverage] Written Cobertura XML report to " +
                reportDir.getAbsolutePath() +
                File.separator +
                "cobertura.xml");
        }

        if (coverageOutputXML) {
            Constructor<ScoverageXmlWriter> cst;
            try {
                cst = ScoverageXmlWriter.class.getConstructor(
                        Class.forName("scala.collection.immutable.Seq"),
                        File.class,
                        boolean.class,
                        Class.forName("scala.Option"));
            } catch (NoSuchMethodException | ClassNotFoundException e) {
                cst = ScoverageXmlWriter.class.getConstructor(
                        Class.forName("scala.collection.Seq"),
                        File.class,
                        boolean.class,
                        Class.forName("scala.Option"));
            }
            ScoverageXmlWriter writer = cst.newInstance(sourceDirsSeq, reportDir, false, new Some<>(sourceEncoding));
            writer.write(coverage);
            logger.info("[scoverage] Written XML report to " +
                reportDir.getAbsolutePath() +
                File.separator +
                Constants.XMLReportFilename());
            if (coverageDebug) {
                ScoverageXmlWriter writerDebug = cst.newInstance(sourceDirsSeq, reportDir, true);
                writerDebug.write(coverage);
                logger.info("[scoverage] Written XML report with debug information to " +
                    reportDir.getAbsolutePath() +
                    File.separator +
                    Constants.XMLReportFilenameWithDebug());
            }
        }

        if (coverageOutputHTML) {
            Constructor<ScoverageHtmlWriter> cst;
            try {
                cst = ScoverageHtmlWriter.class.getConstructor(Class.forName("scala.collection.immutable.Seq"), File.class, Option.class);
            } catch (NoSuchMethodException | ClassNotFoundException e) {
                cst = ScoverageHtmlWriter.class.getConstructor(Class.forName("scala.collection.Seq"), File.class, Option.class);
            }
            ScoverageHtmlWriter writer = cst.newInstance(sourceDirsSeq, reportDir, new Some<>(sourceEncoding));
            writer.write(coverage);
            logger.info("[scoverage] Written HTML report to " +
                reportDir.getAbsolutePath() +
                File.separator +
                "index.html");
        }

        logger.info("[scoverage] Coverage reports completed");
    }
}
