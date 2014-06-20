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

    @TaskAction
    void requireLineCoverage() {
        def extension = ScoveragePlugin.extensionIn(project)

        if (cobertura == null) cobertura = new File(extension.dataDir, 'cobertura.xml')

        def xml = new XmlParser().parse(cobertura)
        def overallLineRate = xml.attribute('line-rate').toDouble()
        def difference = (minimumLineRate - overallLineRate)

        if (difference > 1e-7)
            throw new GradleException("Only ${overallLineRate * 100}% of project is covered by tests instead of ${(minimumLineRate * 100).toInteger()}%!")
    }
}
