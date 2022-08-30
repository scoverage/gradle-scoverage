package org.scoverage

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import scoverage.report.CoverageAggregator

import static org.gradle.api.tasks.PathSensitivity.RELATIVE

@CacheableTask
class ScoverageReport extends DefaultTask {

    @Nested
    ScoverageRunner runner

    @InputDirectory
    @PathSensitive(RELATIVE)
    final Property<File> dataDir = project.objects.property(File)

    @InputFiles
    @PathSensitive(RELATIVE)
    final ConfigurableFileCollection sources = project.objects.fileCollection()

    @OutputDirectory
    final Property<File> reportDir = project.objects.property(File)

    @Input
    final Property<String> sourceEncoding = project.objects.property(String)

    @Input
    final Property<Boolean> coverageOutputCobertura = project.objects.property(Boolean)
    @Input
    final Property<Boolean> coverageOutputXML = project.objects.property(Boolean)
    @Input
    final Property<Boolean> coverageOutputHTML = project.objects.property(Boolean)
    @Input
    final Property<Boolean> coverageDebug = project.objects.property(Boolean)

    @TaskAction
    def report() {
        runner.run(ReportAction.class) { parameters ->
            parameters.dataDir = dataDir
            parameters.sources.from(sources)
            parameters.reportDir = reportDir
            parameters.sourceEncoding = sourceEncoding
            parameters.coverageOutputCobertura = coverageOutputCobertura
            parameters.coverageOutputXML = coverageOutputXML
            parameters.coverageOutputHTML = coverageOutputHTML
            parameters.coverageDebug = coverageDebug
        }
    }

    static interface Parameters extends WorkParameters {
        Property<File> getDataDir()
        ConfigurableFileCollection getSources()
        Property<File> getReportDir()
        Property<String> getSourceEncoding()
        Property<Boolean> getCoverageOutputCobertura()
        Property<Boolean> getCoverageOutputXML()
        Property<Boolean> getCoverageOutputHTML()
        Property<Boolean> getCoverageDebug()
    }

    static abstract class ReportAction implements WorkAction<Parameters> {

        @Override
        void execute() {
            getParameters().reportDir.get().delete()
            getParameters().reportDir.get().mkdirs()

            def coverage = CoverageAggregator.aggregate([getParameters().dataDir.get()] as File[])

            def logger = Logging.getLogger(ReportAction.class)

            if (coverage.isEmpty()) {
                logger.info("[scoverage] Could not find coverage file, skipping...")
            } else {
                new ScoverageWriter(logger).write(
                        getParameters().sources.getFiles(),
                        getParameters().reportDir.get(),
                        coverage.get(),
                        getParameters().sourceEncoding.get(),
                        getParameters().coverageOutputCobertura.get(),
                        getParameters().coverageOutputXML.get(),
                        getParameters().coverageOutputHTML.get(),
                        getParameters().coverageDebug.get())
            }

        }

    }
}
