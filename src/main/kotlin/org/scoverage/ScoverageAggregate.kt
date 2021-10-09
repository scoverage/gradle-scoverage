package org.scoverage

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.TaskAction
import scoverage.report.CoverageAggregator

import org.gradle.api.tasks.PathSensitivity.RELATIVE
import java.io.File

abstract class ScoverageAggregate: DefaultTask() {

    @Nested
    var runner: ScoverageRunner? = null

    @InputFiles
    @PathSensitive(RELATIVE)
    val sources: ConfigurableFileCollection = project.objects.fileCollection()

    @OutputDirectory
    val reportDir: Property<File> = project.objects.property(File::class.java)

    @Input
    val dirsToAggregateFrom: ListProperty<File> = project.objects.listProperty(File::class.java)

    @Input
    val deleteReportsOnAggregation: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    val sourceEncoding: Property<String> = project.objects.property(String::class.java)

    // TODO - consider separate options for `report` and `aggregate` tasks
    @Input
    val coverageOutputCobertura: Property<Boolean> = project.objects.property(Boolean::class.java)
    @Input
    val coverageOutputXML: Property<Boolean> = project.objects.property(Boolean::class.java)
    @Input
    val coverageOutputHTML: Property<Boolean> = project.objects.property(Boolean::class.java)
    @Input
    val coverageDebug: Property<Boolean> = project.objects.property(Boolean::class.java)

    @TaskAction
    fun aggregate() {
        runner?.run {
            reportDir.get().deleteRecursively()
            reportDir.get().mkdirs()

            val dirs = dirsToAggregateFrom.get()
            val uniqueDirs = dirs.toSet()
            val coverage = CoverageAggregator.aggregate(uniqueDirs.toTypedArray())

            if (coverage.nonEmpty()) {
                ScoverageWriter(project.logger).write(
                        sources.files,
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
