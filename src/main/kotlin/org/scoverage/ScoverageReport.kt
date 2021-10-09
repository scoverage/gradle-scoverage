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
import scoverage.report.CoverageAggregator

import org.gradle.api.tasks.PathSensitivity.RELATIVE
import java.io.File

@CacheableTask
abstract class ScoverageReport: DefaultTask() {

    @Nested
    var runner: ScoverageRunner? = null

    @InputDirectory
    @PathSensitive(RELATIVE)
    val dataDir: Property<File> = project.objects.property(File::class.java)

    @InputFiles
    @PathSensitive(RELATIVE)
    val sources: Property<FileCollection> = project.objects.property(FileCollection::class.java)

    @OutputDirectory
    val reportDir: Property<File> = project.objects.property(File::class.java)

    @Input
    val sourceEncoding: Property<String> = project.objects.property(String::class.java)

    @Input
    val coverageOutputCobertura: Property<Boolean> = project.objects.property(Boolean::class.java)
    @Input
    val coverageOutputXML: Property<Boolean> = project.objects.property(Boolean::class.java)
    @Input
    val coverageOutputHTML: Property<Boolean> = project.objects.property(Boolean::class.java)
    @Input
    val coverageDebug: Property<Boolean> = project.objects.property(Boolean::class.java)

    @TaskAction
    fun report() {
        runner?.run {
            reportDir.get().delete()
            reportDir.get().mkdirs()

            val coverage = CoverageAggregator.aggregate(arrayOf(dataDir.get()))

            if (coverage.isEmpty) {
                project.logger.info("[scoverage] Could not find coverage file, skipping...")
            } else {
                ScoverageWriter(project.logger).write(
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
