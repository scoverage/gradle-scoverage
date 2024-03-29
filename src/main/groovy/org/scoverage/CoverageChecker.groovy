package org.scoverage

import groovy.xml.XmlParser
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/**
 * Handles different types of coverage Scoverage can measure.
 */
enum CoverageType {
    Line('Line', 'cobertura.xml', 'line-rate', 1.0),
    Statement('Statement', 'scoverage.xml', 'statement-rate', 100.0),
    Branch('Branch', 'scoverage.xml', 'branch-rate', 100.0)

    /** Name of enum option the way it appears in the build configuration  */
    String configurationName
    /** Name of file with coverage data */
    String fileName
    /** Name of param in XML file with coverage value */
    String paramName
    /** Used to normalize coverage value */
    private double factor

    private CoverageType(String configurationName, String fileName, String paramName, double factor) {
        this.configurationName = configurationName
        this.fileName = fileName
        this.paramName = paramName
        this.factor = factor
    }

    /** Normalize coverage value to [0, 1] */
    Double normalize(Double value) {
        return value / factor
    }

    static CoverageType find(String configurationName) {
        CoverageType.values().find {
            it.configurationName.toLowerCase() == configurationName.toLowerCase()
        }
    }
}

/**
 * Throws a GradleException if overall coverage dips below the configured percentage.
 */
class CoverageChecker {

    final Logger logger

    CoverageChecker(Logger logger) {
        this.logger = logger
    }

    public void checkLineCoverage(File reportDir, CoverageType coverageType, double minimumRate) throws GradleException {
        logger.info("Checking coverage. Type: {}. Minimum rate: {}", coverageType, minimumRate)

        XmlParser parser = new XmlParser()
        parser.setFeature('http://apache.org/xml/features/disallow-doctype-decl', false)
        parser.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)

        try {
            File reportFile = new File(reportDir, coverageType.fileName)
            Node xml = parser.parse(reportFile)
            Double coverageValue = (xml.attribute(coverageType.paramName) as String).toDouble()
            Double overallRate = coverageType.normalize(coverageValue)
            def difference = (minimumRate - overallRate)

            if (difference > 1e-7) {
                throw new GradleException(errorMsg(overallRate, minimumRate, coverageType))
            }
        } catch (FileNotFoundException fnfe) {
            throw new GradleException(fileNotFoundErrorMsg(coverageType), fnfe)
        }
    }

    @VisibleForTesting
    protected static String errorMsg(double overallRate, double minimumRate, CoverageType type) {
        DecimalFormat df = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.US))
        String actual = df.format(overallRate * 100)
        String expected = df.format(minimumRate * 100)
        "Only $actual% of project is covered by tests instead of $expected% (coverageType: $type)"
    }

    @VisibleForTesting
    protected static String fileNotFoundErrorMsg(CoverageType coverageType) {
        "Coverage file (type: $coverageType) not found, check your configuration."
    }
}
