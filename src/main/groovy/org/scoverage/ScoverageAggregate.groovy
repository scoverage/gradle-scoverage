package org.scoverage

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.TaskAction
import scoverage.reporter.CoverageAggregator

import static org.gradle.api.tasks.PathSensitivity.RELATIVE

class ScoverageAggregate extends DefaultTask {

    @Nested
    ScoverageRunner runner

    @InputFiles
    @PathSensitive(RELATIVE)
    final Property<FileCollection> sources = project.objects.property(FileCollection)

    @OutputDirectory
    final Property<File> reportDir = project.objects.property(File)

    @Input
    final ListProperty<File> dirsToAggregateFrom = project.objects.listProperty(File)

    @Input
    final Property<Boolean> deleteReportsOnAggregation = project.objects.property(Boolean)

    @Input
    final Property<String> sourceEncoding = project.objects.property(String)

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
        dirsToAggregateFrom.set([project.extensions.scoverage.dataDir.get()])
    }

    @TaskAction
    def aggregate() {
        runner.run {
            reportDir.get().deleteDir()
            reportDir.get().mkdirs()

            def dirs = []
            dirs.addAll(dirsToAggregateFrom.get())
            def sourceRoot = getProject().getRootDir()
            def coverage = CoverageAggregator.aggregate(dirs.unique() as File[], sourceRoot)

            if (coverage.nonEmpty()) {
                new ScoverageWriter(project.logger).write(
                        sources.get().getFiles(),
                        reportDir.get(),
                        coverage.get(),
                        sourceEncoding.get(),
                        coverageOutputCobertura.get(),
                        coverageOutputXML.get(),
                        coverageOutputHTML.get(),
                        coverageDebug.get()
                )
            }
        }
    }
}
