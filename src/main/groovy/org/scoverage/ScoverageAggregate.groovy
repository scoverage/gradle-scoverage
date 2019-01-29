package org.scoverage

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
import scoverage.report.CoverageAggregator

class ScoverageAggregate extends DefaultTask {

    ScoverageRunner runner

    // TODO - consider separate options for `report` and `aggregate` tasks
    final Property<Boolean> coverageOutputCobertura = project.objects.property(Boolean)
    final Property<Boolean> coverageOutputXML = project.objects.property(Boolean)
    final Property<Boolean> coverageOutputHTML = project.objects.property(Boolean)
    final Property<Boolean> coverageDebug = project.objects.property(Boolean)

    // TODO get these from extension
    boolean clean = false
    File reportDir

    @TaskAction
    def aggregate() {
        runner.run {
            def rootDir = project.projectDir
            def reportPath = reportDir ? reportDir : new File(project.buildDir, 'scoverage-aggregate')

            def coverage = CoverageAggregator.aggregate(rootDir, clean)

            if (coverage.nonEmpty()) {
                ScoverageWriter.write(
                        rootDir,
                        reportPath,
                        coverage.get(),
                        coverageOutputCobertura.get(),
                        coverageOutputXML.get(),
                        coverageOutputHTML.get(),
                        coverageDebug.get()
                )
            }
        }
    }
}
