package com.github.maiflai

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class OverallCheckTask extends DefaultTask {

    File cobertura
    double minimumLineRate = 1

    @TaskAction
    void requireLineCoverage() {

        def reportDirName = project.extensions[ScctPlugin.CONFIGURATION_NAME].reportDirName
        if (cobertura == null){
            cobertura = project.file("$project.buildDir/reports/$reportDirName/cobertura.xml")
        }

        def xml = new XmlParser().parse(cobertura)
        def overallLineRate = xml.attribute('line-rate').toDouble()
        def difference = (minimumLineRate - overallLineRate)
        if (difference > 0.001) throw new GradleException("Line coverage of $overallLineRate is below $minimumLineRate by $difference")
    }

}
