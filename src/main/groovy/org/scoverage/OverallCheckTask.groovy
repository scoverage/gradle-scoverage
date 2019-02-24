package org.scoverage

import org.apache.tools.ant.taskdefs.Local
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.impldep.com.google.common.annotations.VisibleForTesting

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
@CacheableTask
class OverallCheckTask extends DefaultTask {

    /** Type of coverage to check. Available options: Line, Statement and Branch */
    @Input
    final Property<CoverageType> coverageType = project.objects.property(CoverageType)
    @Input
    final Property<BigDecimal> minimumRate = project.objects.property(BigDecimal)

    @Input
    final Property<File> reportDir = project.objects.property(File)

    /** Overwrite to test for a specific locale. */
    @Input
    final Property<Locale> locale = project.objects.property(Locale).value(Locale.getDefault())

    @TaskAction
    void requireLineCoverage() {
        NumberFormat nf = NumberFormat.getInstance(locale.get())

        Exception failure = checkLineCoverage(nf, reportDir.get(), coverageType.get(), minimumRate.get().doubleValue())

        if (failure) throw failure
    }

    @VisibleForTesting
    protected static String errorMsg(String actual, String expected, CoverageType type) {
        return "Only $actual% of project is covered by tests instead of $expected% (coverageType: $type)"
    }

    @VisibleForTesting
    protected static String fileNotFoundErrorMsg(CoverageType coverageType) {
        return "Coverage file (type: $coverageType) not found, check your configuration."
    }

    @VisibleForTesting
    protected static Exception checkLineCoverage(NumberFormat nf, File reportDir, CoverageType coverageType, double minimumRate) {
        XmlParser parser = new XmlParser()
        parser.setFeature('http://apache.org/xml/features/disallow-doctype-decl', false)
        parser.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)

        DecimalFormat df = new DecimalFormat("#.##")

        try {
            File reportFile = new File(reportDir, coverageType.fileName)
            Node xml = parser.parse(reportFile)
            Double coverageValue = nf.parse(xml.attribute(coverageType.paramName) as String).doubleValue()
            Double overallRate = coverageType.normalize(coverageValue)
            def difference = (minimumRate - overallRate)

            if (difference > 1e-7) {
                String is = df.format(overallRate * 100)
                String needed = df.format(minimumRate * 100)
                return new GradleException(errorMsg(is, needed, coverageType))
            }
        } catch (FileNotFoundException fnfe) {
            return new GradleException(fileNotFoundErrorMsg(coverageType), fnfe)
        }
        return null
    }
}
