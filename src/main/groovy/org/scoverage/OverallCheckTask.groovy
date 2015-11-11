package org.scoverage

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

import java.text.DecimalFormat
import java.text.NumberFormat

/**
 * Handles different types of coverage Scoverage can measure.
 */
enum CoverageType {
    Line('cobertura.xml', 'line-rate', 1.0),
    Statement('scoverage.xml', 'statement-rate', 100.0),
    Branch('scoverage.xml', 'branch-rate', 100.0)

    /** Name of file with coverage data */
    String fileName
    /** Name of param in XML file with coverage value */
    String paramName
    /** Used to normalize coverage value */
    private double factor

    private CoverageType(String fileName, String paramName, double factor) {
        this.fileName = fileName
        this.paramName = paramName
        this.factor = factor
    }

    /** Normalize coverage value to [0, 1] */
    Double normalize(Double value) {
        return value / factor
    }
}

/**
 * Throws a GradleException if overall coverage dips below the configured percentage.
 */
class OverallCheckTask extends DefaultTask {

    /** Type of coverage to check. Available options: Line, Statement and Branch */
    CoverageType coverageType = CoverageType.Statement
    double minimumRate = 0.75

    /** Set if want to change default from 'reportDir' in scoverage extension. */
    File reportDir

    protected XmlParser parser
    protected DecimalFormat df = new DecimalFormat("#.##")

    OverallCheckTask() {
        parser = new XmlParser()
        parser.setFeature('http://apache.org/xml/features/disallow-doctype-decl', false)
        parser.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)
    }

    /** Extracted to method for testing purposes */
    static String errorMsg(String actual, String expected, CoverageType type) {
        return "Only $actual% of project is covered by tests instead of $expected% (coverageType: $type)"
    }

    /** Extracted to method for testing purposes */
    static String fileNotFoundErrorMsg(CoverageType coverageType) {
        return "Coverage file (type: $coverageType) not found, check your configuration."
    }

    @TaskAction
    void requireLineCoverage() {
        def extension = ScoveragePlugin.extensionIn(project)

        File reportFile = new File(reportDir ? reportDir : extension.reportDir, coverageType.fileName)

        try {
            Node xml = parser.parse(reportFile)
            NumberFormat nf = NumberFormat.getInstance();
            Double coverageValue = nf.parse(xml.attribute(coverageType.paramName) as String).doubleValue();
            Double overallRate = coverageType.normalize(coverageValue)
            def difference = (minimumRate - overallRate)

            if (difference > 1e-7) {
                String is = df.format(overallRate * 100)
                String needed = df.format(minimumRate * 100)
                throw new GradleException(errorMsg(is, needed, coverageType))
            }
        } catch (FileNotFoundException fnfe) {
            throw new GradleException(fileNotFoundErrorMsg(coverageType), fnfe)
        }

    }
}
