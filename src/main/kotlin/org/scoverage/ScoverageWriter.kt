package org.scoverage

import org.gradle.api.logging.Logger
import scala.Option
import scala.Some
import scoverage.Constants
import scoverage.Coverage
import scoverage.report.CoberturaXmlWriter
import scoverage.report.ScoverageHtmlWriter
import scoverage.report.ScoverageXmlWriter
import java.io.File
import java.lang.reflect.Constructor

/**
 * Util for generating and saving coverage files.
 * Copied from sbt-scoverage and converted to Java to avoid dependency to Scala.
 */
class ScoverageWriter(private val logger: Logger) {

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
    @Throws(ReflectiveOperationException::class)
    fun write(
        sourceDirs: Set<File>,
        reportDir: File,
        coverage: Coverage,
        sourceEncoding: String,
        coverageOutputCobertura: Boolean,
        coverageOutputXML: Boolean,
        coverageOutputHTML: Boolean,
        coverageDebug: Boolean
    ) {

        logger.info("[scoverage] Generating scoverage reports...")

        reportDir.mkdirs()

        val scalaBuffer = Class.forName("scala.collection.JavaConverters")
            .getMethod(
                "asScalaBuffer", java.util.List::class.java
            )
            .invoke(null, listOf(*sourceDirs.toTypedArray()))
        val sourceDirsSeq = scalaBuffer.javaClass.getMethod("toIndexedSeq").invoke(scalaBuffer)

        if (coverageOutputCobertura) {
            val cst: Constructor<CoberturaXmlWriter> = try {
                CoberturaXmlWriter::class.java.getConstructor(
                    Class.forName("scala.collection.immutable.Seq"),
                    File::class.java
                )
            } catch (e: ReflectiveOperationException) {
                CoberturaXmlWriter::class.java.getConstructor(
                    Class.forName("scala.collection.Seq"),
                    File::class.java
                )
            }
            val writer = cst.newInstance(sourceDirsSeq, reportDir)
            writer.write(coverage)
            logger.info(
                "[scoverage] Written Cobertura XML report to " +
                        reportDir.absolutePath +
                        File.separator +
                        "cobertura.xml"
            )
        }

        if (coverageOutputXML) {
            val cst: Constructor<ScoverageXmlWriter> = try {
                ScoverageXmlWriter::class.java.getConstructor(
                    Class.forName("scala.collection.immutable.Seq"),
                    File::class.java,
                    Boolean::class.java
                )
            } catch (e: ReflectiveOperationException) {
                ScoverageXmlWriter::class.java.getConstructor(
                    Class.forName("scala.collection.Seq"),
                    File::class.java,
                    Boolean::class.java
                )
            }
            val writer = cst.newInstance(sourceDirsSeq, reportDir, false)
            writer.write(coverage)
            logger.info(
                "[scoverage] Written XML report to " +
                        reportDir.absolutePath +
                        File.separator +
                        Constants.XMLReportFilename()
            )
            if (coverageDebug) {
                val writerDebug = cst.newInstance(sourceDirsSeq, reportDir, true)
                writerDebug.write(coverage)
                logger.info(
                    "[scoverage] Written XML report with debug information to " +
                            reportDir.absolutePath +
                            File.separator +
                            Constants.XMLReportFilenameWithDebug()
                )
            }
        }

        if (coverageOutputHTML) {
            val cst: Constructor<ScoverageHtmlWriter> = try {
                ScoverageHtmlWriter::class.java.getConstructor(
                    Class.forName("scala.collection.immutable.Seq"),
                    File::class.java,
                    Option::class.java
                )
            } catch (e: ReflectiveOperationException) {
                ScoverageHtmlWriter::class.java.getConstructor(
                    Class.forName("scala.collection.Seq"),
                    File::class.java,
                    Option::class.java
                )
            }
            val writer = cst.newInstance(sourceDirsSeq, reportDir, Some(sourceEncoding))
            writer.write(coverage)
            logger.info(
                "[scoverage] Written HTML report to " +
                        reportDir.absolutePath +
                        File.separator +
                        "index.html"
            )
        }

        logger.info("[scoverage] Coverage reports completed")
    }
}
