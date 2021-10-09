package org.scoverage

import groovy.xml.XmlParser
import org.gradle.api.GradleException
import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting
import org.slf4j.Logger
import java.io.File
import java.io.FileNotFoundException

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import kotlin.jvm.Throws

/**
 * Handles different types of coverage Scoverage can measure.
 *
 * @param configurationName Name of enum option the way it appears in the build configuration.
 * @param fileName Name of file with coverage data.
 * @param paramName Name of param in XML file with coverage value.
 * @param factor Used to normalize coverage value.
 */
enum class CoverageType(
    private val configurationName: String,
    val fileName: String,
    val paramName: String,
    private val factor: Double
) {
    Line("Line", "cobertura.xml", "line-rate", 1.0),
    Statement("Statement", "scoverage.xml", "statement-rate", 100.0),
    Branch("Branch", "scoverage.xml", "branch-rate", 100.0);

    /** Normalize coverage value to [0, 1] */
    fun normalize(value: Double): Double = value / factor

    companion object {
        fun find(configurationName: String): CoverageType? {
            return values().find { it -> it.configurationName.equals(configurationName, ignoreCase = true) }
        }
    }
}

/**
 * Throws a GradleException if overall coverage dips below the configured percentage.
 */
class CoverageChecker(private val logger: Logger) {

    @JvmOverloads
    @Throws(GradleException::class)
    fun checkLineCoverage(
        reportDir: File,
        coverageType: CoverageType,
        minimumRate: Double,
        nf: NumberFormat = NumberFormat.getInstance(Locale.getDefault())
    ) {
        logger.info("Checking coverage. Type: {}. Minimum rate: {}", coverageType, minimumRate)

        val parser = XmlParser()
        parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
        parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)

        val df = DecimalFormat("#.##")

        try {
            val reportFile = File(reportDir, coverageType.fileName)
            val xml = parser.parse(reportFile)
            val coverageValue: Double = nf.parse(xml.attribute(coverageType.paramName) as String).toDouble()
            val overallRate: Double = coverageType.normalize(coverageValue)

            val difference = minimumRate - overallRate

            if (difference > 1e-7) {
                val iss = df.format(overallRate * 100)
                val needed = df.format(minimumRate * 100)
                throw GradleException(errorMsg(iss, needed, coverageType))
            }
        } catch (fnfe: FileNotFoundException) {
            throw GradleException(fileNotFoundErrorMsg(coverageType), fnfe)
        }
    }

    companion object {
        @VisibleForTesting
        internal fun errorMsg(actual: String, expected: String, type: CoverageType): String {
            return "Only $actual% of project is covered by tests instead of $expected% (coverageType: $type)"
        }

        @VisibleForTesting
        internal fun fileNotFoundErrorMsg(coverageType: CoverageType): String {
            return "Coverage file (type: $coverageType) not found, check your configuration."
        }
    }

}
