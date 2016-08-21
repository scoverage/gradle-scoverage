package org.scoverage

import static org.hamcrest.number.IsCloseTo.closeTo
import org.junit.Test

import static org.junit.Assert.assertThat

class SeparateTestsAcceptanceTest extends AcceptanceTestUtils {

    @Test
    public void testSeparateTestsWithZinc() throws Exception {
        File projectDir = new File('src/test/separate-tests')
        File subprojectDir = new File(projectDir, 'a')
        File testsSubprojectDir = new File(projectDir, 'a-tests')
        def build = setupBuild(projectDir)
        build.forTasks('clean', 'reportScoverage').run()
        def indexHtml = new File(reportDir(subprojectDir), 'index.html')
        checkFile('an index HTML file', indexHtml, true)
        def testsIndexHtml = new File(reportDir(testsSubprojectDir), 'index.html')
        checkFile('an index HTML file', testsIndexHtml, false)
        def helloHtml = new File(reportDir(subprojectDir), 'src/main/scala/hello/Hello.scala.html')
        checkFile('Hello.scala html file', helloHtml, true)
        def branchCoverage = coverage(reportDir(subprojectDir), CoverageType.Branch)
        def statementCoverage = coverage(reportDir(subprojectDir), CoverageType.Statement)
        assertThat('Branch coverage should be 100%, was ' + branchCoverage, branchCoverage, closeTo(100.0, 1.0))
        assertThat('Statement coverage should be 100%, was ' + statementCoverage, statementCoverage, closeTo(100.0, 1.0))
    }
}
