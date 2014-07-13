package org.scoverage

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Throws a GradleException if overall line coverage dips below the configured percentage.
 */
class OverallCheckTask extends DefaultTask {
    File cobertura
    double minimumLineRate = 0.75

    protected XmlParser parser;

    OverallCheckTask() {
        parser = new XmlParser()
        parser.setFeature('http://apache.org/xml/features/disallow-doctype-decl', false)
        parser.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)
    }

    @TaskAction
    void requireLineCoverage() {
        def extension = ScoveragePlugin.extensionIn(project)

        if (cobertura == null) cobertura = new File(extension.reportDir, 'cobertura.xml')

        def xml = parser.parse(cobertura)
        def overallLineRate = xml.attribute('line-rate').toDouble()
        def difference = (minimumLineRate - overallLineRate)

        if (difference > 1e-7)
            throw new GradleException("Only ${overallLineRate * 100}% of project is covered by tests instead of ${(minimumLineRate * 100).toInteger()}%!")
    }
}
