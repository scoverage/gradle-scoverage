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
class ScoverageReport extends DefaultTask {

    @Nested
    ScoverageRunner runner

    @InputDirectory
    @PathSensitive(RELATIVE)
    final Property<File> dataDir = project.objects.property(File)

    @InputFiles
    @PathSensitive(RELATIVE)
    final Property<FileCollection> sources = project.objects.property(FileCollection)

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
        runner.run {
            reportDir.get().delete()
            reportDir.get().mkdirs()

            def sourceRoot = getProject().getRootDir()
            def coverage = CoverageAggregator.aggregate([dataDir.get()] as File[], sourceRoot)

            if (coverage.isEmpty()) {
                project.logger.info("[scoverage] Could not find coverage file, skipping...")
            } else {
                new ScoverageWriter(project.logger).write(
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
