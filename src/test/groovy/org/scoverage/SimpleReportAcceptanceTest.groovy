package org.scoverage

import org.junit.Test

class SimpleReportAcceptanceTest extends AcceptanceTestUtils {

    private testHappyDay(boolean useAnt) throws Exception {
        File projectRoot = new File('src/test/happy day')
        def build = setupBuild(projectRoot, useAnt)

        build.forTasks('clean', 'checkScoverage').run()

        def html = new File(reportDir(projectRoot), 'index.html')
        checkFile('an index HTML file', html, true)
        def cobertura = new File(reportDir(projectRoot), 'cobertura.xml')
        checkFile('a cobertura XML file', cobertura, true)
        def scoverageXml = new File(reportDir(projectRoot), 'scoverage.xml')
        checkFile('a scoverage XML file', scoverageXml, true)
    }

    @Test
    public void testAntProjectWithCompleteCoverage() throws Exception {
        testHappyDay(true)
    }

    @Test
    public void testZincProjectWithCompleteCoverage() throws Exception {
        testHappyDay(false)
    }

}
