package org.scoverage

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
// don't use scala.collection.JavaConverters as it breaks backward compatibility with scala 2.11
import scala.collection.JavaConversions
import scoverage.IOUtils
import scoverage.report.CoverageAggregator

class ScoverageAggregate extends DefaultTask {

    ScoverageRunner runner

    @OutputDirectory
    final Property<File> reportDir = project.objects.property(File)

    @Input
    final ListProperty<File> dirsToAggregateFrom = project.objects.listProperty(File)

    @Input
    final Property<Boolean> deleteReportsOnAggregation = project.objects.property(Boolean)

    // TODO - consider separate options for `report` and `aggregate` tasks
    @Input
    final Property<Boolean> coverageOutputCobertura = project.objects.property(Boolean)
    @Input
    final Property<Boolean> coverageOutputXML = project.objects.property(Boolean)
    @Input
    final Property<Boolean> coverageOutputHTML = project.objects.property(Boolean)
    @Input
    final Property<Boolean> coverageDebug = project.objects.property(Boolean)

    ScoverageAggregate() {
        dirsToAggregateFrom.set([])
    }

    @TaskAction
    def aggregate() {
        runner.run {
            def rootDir = project.projectDir

            def coverage
            if (dirsToAggregateFrom.get().isEmpty()) {
                coverage = CoverageAggregator.aggregate(rootDir, deleteReportsOnAggregation.get())
            } else {
                def reportFiles = dirsToAggregateFrom.get().collectMany {
                    JavaConversions.seqAsJavaList(IOUtils.reportFileSearch(it, IOUtils.isReportFile()))
                }
                coverage = CoverageAggregator.aggregate(JavaConversions.asScalaBuffer(reportFiles), deleteReportsOnAggregation.get())
            }

            reportDir.get().deleteDir()

            if (coverage.nonEmpty()) {
                new ScoverageWriter(project.logger).write(
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
