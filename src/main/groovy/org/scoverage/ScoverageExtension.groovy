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

    /** Version of scoverage to use for the scalac plugin */
    final Property<String> scoverageVersion

    /** Version of scala to use for the scalac plugin */
    final Property<String> scoverageScalaVersion

    /** a directory to write working files to */
    final Property<File> dataDir
    /** a directory to write final output to */
    final Property<File> reportDir
    /** sources to highlight */
    final Property<File> sources
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

    final Property<Boolean> runNormalCompilation

    final Property<CoverageType> coverageType
    final Property<BigDecimal> minimumRate

    ScoverageExtension(Project project) {

        project.plugins.apply(JavaPlugin.class)
        project.plugins.apply(ScalaPlugin.class)

        scoverageVersion = project.objects.property(String)
        scoverageVersion.set('1.3.1')

        scoverageScalaVersion = project.objects.property(String)
        scoverageScalaVersion.set('2.12')

        sources = project.objects.property(File)
        sources.set(project.projectDir)

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

        runNormalCompilation = project.objects.property(Boolean)
        runNormalCompilation.set(true)

        coverageType = project.objects.property(CoverageType)
        coverageType.set(CoverageType.Statement)

        minimumRate = project.objects.property(BigDecimal)
        minimumRate.set(0.75)
    }
}
