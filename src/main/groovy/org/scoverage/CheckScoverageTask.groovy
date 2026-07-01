package org.scoverage

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.TaskAction

import static org.gradle.api.tasks.PathSensitivity.RELATIVE

@CacheableTask
abstract class CheckScoverageTask extends DefaultTask {

    static class CheckSpec implements Serializable {
        final String coverageTypeName
        final BigDecimal minimumRate

        CheckSpec(String coverageTypeName, BigDecimal minimumRate) {
            this.coverageTypeName = coverageTypeName
            this.minimumRate = minimumRate
        }
    }

    @InputDirectory
    @PathSensitive(RELATIVE)
    abstract Property<File> getReportDir()

    @Input
    abstract ListProperty<CheckSpec> getChecks()

    @TaskAction
    void checkCoverage() {
        def checker = new CoverageChecker(logger)
        checks.get().each { config ->
            def coverageType = CoverageType.find(config.coverageTypeName)
            if (coverageType == null) {
                throw new IllegalArgumentException("Unknown coverage type: ${config.coverageTypeName}")
            }
            checker.checkLineCoverage(reportDir.get(), coverageType, config.minimumRate.doubleValue())
        }
    }
}
