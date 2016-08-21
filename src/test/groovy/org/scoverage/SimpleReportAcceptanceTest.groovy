package org.scoverage

import org.junit.Test

class SimpleReportAcceptanceTest extends AcceptanceTestUtils {


    @Test
    public void testZincProjectWithCompleteCoverage() throws Exception {
        File projectRoot = new File('src/test/happy day')
        def build = setupBuild(projectRoot)
        build.forTasks('clean', 'checkScoverage').run()
        def html = new File(reportDir(projectRoot), 'index.html')
        checkFile('an index HTML file', html, true)
        def cobertura = new File(reportDir(projectRoot), 'cobertura.xml')
        checkFile('a cobertura XML file', cobertura, true)
        def scoverageXml = new File(reportDir(projectRoot), 'scoverage.xml')
        checkFile('a scoverage XML file', scoverageXml, true)
    }

    @Test
    public void testRun() throws Exception {
        File projectRoot = new File('src/test/runtime')
        def build = setupBuild(projectRoot)
        build.forTasks('clean', 'run').run()
    }
}
