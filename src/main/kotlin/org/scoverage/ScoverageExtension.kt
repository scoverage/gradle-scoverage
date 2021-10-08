package org.scoverage

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.io.File
import java.math.BigDecimal

/**
 * Defines a new SourceSet for the code to be instrumented.
 * Defines a new Test Task which executes normal tests with the instrumented classes.
 * Defines a new Check Task which enforces an overall line coverage requirement.
 */
abstract class ScoverageExtension(val project: Project) {

    companion object {
        @JvmField
        val DEFAULT_REPORT_DIR = "reports" + File.separatorChar + "scoverage"
        @JvmField
        val DEFAULT_COVERAGE_TYPE: CoverageType = CoverageType.Statement
        const val DEFAULT_MINIMUM_RATE: Double = 0.75
    }

    /** Version of scoverage to use for the scalac plugin */
    val scoverageVersion: Property<String> = project.objects.property(String::class.java)

    /** Version of scala to use for the scalac plugin */
    val scoverageScalaVersion: Property<String> = project.objects.property(String::class.java)

    /** a directory to write working files to */
    val dataDir: Property<File> = project.objects.property(File::class.java)
    /** a directory to write final output to */
    val reportDir: Property<File> = project.objects.property(File::class.java)
    /** range positioning for highlighting */
    val highlighting: Property<Boolean> = project.objects.property(Boolean::class.java)
    /** regex for each excluded package */
    val excludedPackages: ListProperty<String> = project.objects.listProperty(String::class.java)
    /** regex for each excluded file */
    val excludedFiles: ListProperty<String> = project.objects.listProperty(String::class.java)

    /** Options for enabling and disabling output */
    val coverageOutputCobertura: Property<Boolean> = project.objects.property(Boolean::class.java)
    val coverageOutputXML: Property<Boolean> = project.objects.property(Boolean::class.java)
    val coverageOutputHTML: Property<Boolean> = project.objects.property(Boolean::class.java)
    val coverageDebug: Property<Boolean> = project.objects.property(Boolean::class.java)

    val deleteReportsOnAggregation: Property<Boolean> = project.objects.property(Boolean::class.java)

    val checks: MutableList<CheckConfig> = mutableListOf()

    val coverageType: Property<CoverageType> = project.objects.property(CoverageType::class.java)
    val minimumRate: Property<BigDecimal> = project.objects.property(BigDecimal::class.java)

    init {
        project.plugins.apply(JavaPlugin::class.java)
        project.plugins.apply(ScalaPlugin::class.java)

        scoverageVersion.set("1.4.8")

        dataDir.set(File(project.buildDir, "scoverage"))
        reportDir.set(File(project.buildDir, DEFAULT_REPORT_DIR))

        highlighting.set(true)

        excludedPackages.set(listOf())
        excludedFiles.set(listOf())

        coverageOutputCobertura.set(true)
        coverageOutputXML.set(true)
        coverageOutputHTML.set(true)
        coverageDebug.set(false)

        deleteReportsOnAggregation.set(false)
    }

    fun check(closure: Closure<*>) {
        val check = CheckConfig()
        project.configure(check, closure)
        checks.add(check)
    }

    data class CheckConfig(var coverageType: CoverageType? = null, var minimumRate: BigDecimal? = null)
}
