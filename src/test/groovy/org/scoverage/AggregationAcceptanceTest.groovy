package org.scoverage

import org.junit.Test

class AggregationAcceptanceTest extends AcceptanceTestUtils {

    @Test
    public void testMultiProjectAggregation() throws Exception {
        File projectDir = new File('src/test/water')
        runBuild(projectDir, 'clean', 'aggregateScoverage')
        def indexHtml = new File(aggregateReportDir(projectDir), 'index.html')
        checkFile('an aggregated index HTML file', indexHtml, true)
        def cobertura = new File(aggregateReportDir(projectDir), 'cobertura.xml')
        checkFile('an aggregated cobertura XML file', cobertura, true)
        def scoverageXml = new File(aggregateReportDir(projectDir), 'scoverage.xml')
        checkFile('an aggregated scoverage XML file', scoverageXml, true)
        def krillsHtml = new File(aggregateReportDir(projectDir), 'krills.html')
        checkFile('a HTML file for \'krills\' sub-project', krillsHtml, true)
        def whalesHtml = new File(aggregateReportDir(projectDir), 'whales.html')
        checkFile('a HTML file for \'whales\' sub-project', whalesHtml, true)
    }

    private static File aggregateReportDir(File baseDir) {
        return new File(baseDir, 'build/scoverage-aggregate')
    }
}
