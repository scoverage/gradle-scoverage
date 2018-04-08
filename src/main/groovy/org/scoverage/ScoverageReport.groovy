package org.scoverage

import org.gradle.api.tasks.JavaExec

class ScoverageReport extends JavaExec {

    @Override
    void exec() {
        def extension = ScoveragePlugin.extensionIn(project)
        extension.reportDir.mkdirs()
        String encoding = ScoveragePlugin.encoding(project)
        if (encoding) {
            jvmArgs("-Dfile.encoding=$encoding")
        }
        setClasspath(extension.pluginClasspath)
        setMain('org.scoverage.SingleReportApp')
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
