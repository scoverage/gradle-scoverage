package org.scoverage

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.TaskAction
import scoverage.reporter.CoverageAggregator

import static org.gradle.api.tasks.PathSensitivity.RELATIVE

@CacheableTask
abstract class ScoverageAggregate extends DefaultTask {

    @Nested
    abstract Property<ScoverageRunner> getRunner()

    @InputFiles
    @PathSensitive(RELATIVE)
    abstract Property<FileCollection> getSources()

    @OutputDirectory
    abstract Property<File> getReportDir()

    @Input
    abstract ListProperty<File> getDirsToAggregateFrom()

    @Input
    abstract Property<Boolean> getDeleteReportsOnAggregation()

    @Input
    abstract Property<String> getSourceEncoding()

    // TODO - consider separate options for `report` and `aggregate` tasks
    @Input
    abstract Property<Boolean> getCoverageOutputCobertura()

    @Input
    abstract Property<Boolean> getCoverageOutputXML()

    @Input
    abstract Property<Boolean> getCoverageOutputHTML()

    @Input
    abstract Property<Boolean> getCoverageDebug()

    @Input
    abstract Property<File> getSourceRoot()

    @TaskAction
    def aggregate() {
        runner.get().run {
            reportDir.get().deleteDir()
            reportDir.get().mkdirs()

            def dirs = []
            dirs.addAll(dirsToAggregateFrom.get())
            def coverage = CoverageAggregator.aggregate(dirs.unique() as File[], sourceRoot.get())

            if (coverage.nonEmpty()) {
                new ScoverageWriter(getLogger()).write(
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
