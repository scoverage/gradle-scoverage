package org.scoverage

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Defines a new SourceSet for the code to be instrumented.
 * Defines a new Test Task which executes normal tests with the instrumented classes.
 * Defines a new Check Task which enforces an overall line coverage requirement.
 */
class ScoverageExtension {

    public static final CoverageType DEFAULT_COVERAGE_TYPE = CoverageType.Statement
    public static final double DEFAULT_MINIMUM_RATE = 0.75

    final Project project

    /** Version of scoverage to use for the scalac plugin */
    final Property<String> scoverageVersion

    /** Version of scala to use for the scalac plugin */
    final Property<String> scoverageScalaVersion

    /** a directory to write working files to */
    final Property<File> dataDir
    /** a directory to write final output to */
    final Property<File> reportDir
    /** range positioning for highlighting */
    final Property<Boolean> highlighting
    /** regex for each excluded package */
    final ListProperty<String> excludedPackages
    /** regex for each excluded file */
    final ListProperty<String> excludedFiles

    /** Options for enabling and disabling output */
    final Property<Boolean> coverageOutputCobertura
    final Property<Boolean> coverageOutputXML
    final Property<Boolean> coverageOutputHTML
    final Property<Boolean> coverageDebug

    final Property<Boolean> deleteReportsOnAggregation

    final List<CheckConfig> checks = new ArrayList<>()

    final Property<CoverageType> coverageType
    final Property<BigDecimal> minimumRate

    ScoverageExtension(Project project) {

        this.project = project
        project.plugins.apply(JavaPlugin.class)
        project.plugins.apply(ScalaPlugin.class)

        scoverageVersion = project.objects.property(String)
        scoverageVersion.set('2.0.8')

        scoverageScalaVersion = project.objects.property(String)

        dataDir = project.objects.property(File)
        dataDir.set(new File(project.buildDir, 'scoverage'))

        reportDir = project.objects.property(File)
        reportDir.set(new File(project.buildDir, ScoveragePlugin.DEFAULT_REPORT_DIR))

        highlighting = project.objects.property(Boolean)
        highlighting.set(true)

        excludedPackages = project.objects.listProperty(String)
        excludedPackages.set([])

        excludedFiles = project.objects.listProperty(String)
        excludedFiles.set([])

        coverageOutputCobertura = project.objects.property(Boolean)
        coverageOutputCobertura.set(true)

        coverageOutputXML = project.objects.property(Boolean)
        coverageOutputXML.set(true)

        coverageOutputHTML = project.objects.property(Boolean)
        coverageOutputHTML.set(true)

        coverageDebug = project.objects.property(Boolean)
        coverageDebug.set(false)

        deleteReportsOnAggregation = project.objects.property(Boolean)
        deleteReportsOnAggregation.set(false)

        coverageType = project.objects.property(CoverageType)
        minimumRate = project.objects.property(BigDecimal)
    }

    void check(Closure closure) {
        CheckConfig check = new CheckConfig()
        project.configure(check, closure)
        checks.add(check)
    }

    static class CheckConfig {
        CoverageType coverageType
        BigDecimal minimumRate
        CheckConfig() {
        }
    }
}
