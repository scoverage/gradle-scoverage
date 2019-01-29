package org.scoverage

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import scoverage.report.CoverageAggregator

class ScoverageAggregate extends DefaultTask {

    ScoverageRunner runner

    @OutputDirectory
    final Property<File> reportDir = project.objects.property(File)

    final Property<Boolean> deleteReportsOnAggregation = project.objects.property(Boolean)

    // TODO - consider separate options for `report` and `aggregate` tasks
    final Property<Boolean> coverageOutputCobertura = project.objects.property(Boolean)
    final Property<Boolean> coverageOutputXML = project.objects.property(Boolean)
    final Property<Boolean> coverageOutputHTML = project.objects.property(Boolean)
    final Property<Boolean> coverageDebug = project.objects.property(Boolean)

    @TaskAction
    def aggregate() {
        runner.run {
            def rootDir = project.projectDir

            def coverage = CoverageAggregator.aggregate(rootDir, deleteReportsOnAggregation.get())

            reportDir.get().deleteDir()

            if (coverage.nonEmpty()) {
                ScoverageWriter.write(
                        rootDir,
                        reportDir.get(),
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
