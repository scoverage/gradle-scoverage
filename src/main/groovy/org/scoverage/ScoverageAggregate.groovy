package org.scoverage

import org.gradle.api.tasks.JavaExec

class ScoverageAggregate extends JavaExec {

    boolean clean = false
    File reportDir

    @Override
    void exec() {
        setClasspath(ScoveragePlugin.extensionIn(project).pluginClasspath)
        setMain('org.scoverage.AggregateReportApp')
        def reportPath = reportDirOrDefault()
        setArgs([project.projectDir, reportPath.absolutePath, clean])
        super.exec()
        def reportEntryPoint = new File(reportPath, 'index.html').absolutePath
        project.logger.lifecycle("Wrote aggregated scoverage report to ${reportEntryPoint}")
    }

    def reportDirOrDefault() {
        return reportDir ? reportDir : new File(project.buildDir, 'scoverage-aggregate')
    }
}
