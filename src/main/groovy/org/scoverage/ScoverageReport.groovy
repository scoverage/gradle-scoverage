package org.scoverage

import org.gradle.api.tasks.JavaExec

class ScoverageReport extends JavaExec {

    @Override
    void exec() {
        def extension = ScoveragePlugin.extensionIn(project)
        extension.reportDir.mkdirs()
        setClasspath(extension.pluginClasspath)
        setMain('org.scoverage.SingleReportApp')
        if (extension.encoding) {
            jvmArgs("-Dfile.encoding=$extension.encoding")
        }
        setArgs([
            /* sourceDir = */ extension.sources.absolutePath,
            /* dataDir = */ extension.dataDir.absolutePath,
            /* reportDir = */ extension.reportDir.absolutePath,
            extension.coverageOutputCobertura,
            extension.coverageOutputXML,
            extension.coverageOutputHTML,
            extension.coverageDebug
        ])
        super.exec()
    }
}
