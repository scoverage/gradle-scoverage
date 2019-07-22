package org.scoverage

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
// don't use scala.collection.JavaConverters as it breaks backward compatibility with scala 2.11
import scala.collection.JavaConversions
import scoverage.report.CoverageAggregator

@CacheableTask
class ScoverageReport extends DefaultTask {

    ScoverageRunner runner

    @Input
    final Property<File> dataDir = project.objects.property(File)

    @Input
    final Property<File> sources = project.objects.property(File)

    @OutputDirectory
    final Property<File> reportDir = project.objects.property(File)

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

            def coverage = CoverageAggregator.aggregate(JavaConversions.asScalaBuffer(Arrays.asList(dataDir.get())))

            if (coverage.isEmpty()) {
                project.logger.info("[scoverage] Could not find coverage file, skipping...")
            } else {
                new ScoverageWriter(project.logger).write(
                        sources.get(),
                        reportDir.get(),
                        coverage.get(),
                        coverageOutputCobertura.get(),
                        coverageOutputXML.get(),
                        coverageOutputHTML.get(),
                        coverageDebug.get())
            }
        }
    }
}
