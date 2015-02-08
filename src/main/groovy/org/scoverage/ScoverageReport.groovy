package org.scoverage

import org.gradle.api.tasks.JavaExec

class ScoverageReport extends JavaExec {

    @Override
    void exec() {
        def extension = ScoveragePlugin.extensionIn(project)
        extension.reportDir.mkdirs()
        setClasspath(extension.pluginClasspath)
        setMain('org.scoverage.SingleReportApp')
        setArgs([extension.sources.absolutePath, extension.dataDir.absolutePath, extension.reportDir.absolutePath])
        super.exec()
    }
}
