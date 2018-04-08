package org.scoverage

import org.gradle.api.tasks.JavaExec

class ScoverageAggregate extends JavaExec {

    boolean clean = false
    File reportDir

    @Override
    void exec() {
        def extension = ScoveragePlugin.extensionIn(project)
        setClasspath(ScoveragePlugin.extensionIn(project).pluginClasspath)
        setMain('org.scoverage.AggregateReportApp')
        String encoding = ScoveragePlugin.encoding(project)
        if (encoding) {
            jvmArgs("-Dfile.encoding=$encoding")
        }
        def reportPath = reportDirOrDefault()
        setArgs([
            project.projectDir,
            reportPath.absolutePath,
            clean,
            // TODO - consider separate options for `report` and `aggregate` tasks
            extension.coverageOutputCobertura,
            extension.coverageOutputXML,
            extension.coverageOutputHTML,
            extension.coverageDebug
        ])
        super.exec()
    }

    def reportDirOrDefault() {
        return reportDir ? reportDir : new File(project.buildDir, 'scoverage-aggregate')
    }
}
