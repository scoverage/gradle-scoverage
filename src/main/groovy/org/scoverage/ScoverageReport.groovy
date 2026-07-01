package org.scoverage

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.TaskAction
import scoverage.reporter.CoverageAggregator

import static org.gradle.api.tasks.PathSensitivity.RELATIVE

@CacheableTask
abstract class ScoverageReport extends DefaultTask {

    @Nested
    abstract Property<ScoverageRunner> getRunner()

    @InputDirectory
    @PathSensitive(RELATIVE)
    abstract Property<File> getDataDir()

    @InputFiles
    @PathSensitive(RELATIVE)
    abstract Property<FileCollection> getSources()

    @OutputDirectory
    abstract Property<File> getReportDir()

    @Input
    abstract Property<String> getSourceEncoding()

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
    def report() {
        runner.get().run {
            reportDir.get().delete()
            reportDir.get().mkdirs()

            def coverage = CoverageAggregator.aggregate([dataDir.get()] as File[], sourceRoot.get())

            if (coverage.isEmpty()) {
                getLogger().info("[scoverage] Could not find coverage file, skipping...")
            } else {
                new ScoverageWriter(getLogger()).write(
                        sources.get().getFiles(),
                        reportDir.get(),
                        coverage.get(),
                        sourceEncoding.get(),
                        coverageOutputCobertura.get(),
                        coverageOutputXML.get(),
                        coverageOutputHTML.get(),
                        coverageDebug.get())
            }
        }
    }
}
