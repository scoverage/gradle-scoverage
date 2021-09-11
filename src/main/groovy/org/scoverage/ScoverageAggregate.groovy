package org.scoverage

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import scoverage.report.CoverageAggregator

import static org.gradle.api.tasks.PathSensitivity.RELATIVE

class ScoverageAggregate extends DefaultTask {

    @Nested
    ScoverageRunner runner

    @InputFiles
    @PathSensitive(RELATIVE)
    final ConfigurableFileCollection sources = project.objects.fileCollection()

    @OutputDirectory
    final Property<File> reportDir = project.objects.property(File)

    @Input
    final ListProperty<File> dirsToAggregateFrom = project.objects.listProperty(File)

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
        runner.run(AggregateAction.class) { parameters ->
            parameters.sources.from(sources)
            parameters.reportDir = reportDir
            parameters.dirsToAggregateFrom = dirsToAggregateFrom
            parameters.sourceEncoding = sourceEncoding
            parameters.coverageOutputCobertura = coverageOutputCobertura
            parameters.coverageOutputXML = coverageOutputXML
            parameters.coverageOutputHTML = coverageOutputHTML
            parameters.coverageDebug = coverageDebug
        }
    }

    static interface Parameters extends WorkParameters {
        ConfigurableFileCollection getSources()
        Property<File> getReportDir()
        ListProperty<File> getDirsToAggregateFrom()
        Property<String> getSourceEncoding()
        Property<Boolean> getCoverageOutputCobertura()
        Property<Boolean> getCoverageOutputXML()
        Property<Boolean> getCoverageOutputHTML()
        Property<Boolean> getCoverageDebug()
    }

    static abstract class AggregateAction implements WorkAction<Parameters> {

        @Override
        void execute() {
            def logger = Logging.getLogger(AggregateAction.class)

            getParameters().reportDir.get().deleteDir()
            getParameters().reportDir.get().mkdirs()

            def dirs = []
            dirs.addAll(getParameters().dirsToAggregateFrom.get())
            def coverage = CoverageAggregator.aggregate(dirs.unique() as File[])

            if (coverage.nonEmpty()) {
                new ScoverageWriter(logger).write(
                        getParameters().sources.getFiles(),
                        getParameters().reportDir.get(),
                        coverage.get(),
                        getParameters().sourceEncoding.get(),
                        getParameters().coverageOutputCobertura.get(),
                        getParameters().coverageOutputXML.get(),
                        getParameters().coverageOutputHTML.get(),
                        getParameters().coverageDebug.get()
                )
            }
        }
    }
}
