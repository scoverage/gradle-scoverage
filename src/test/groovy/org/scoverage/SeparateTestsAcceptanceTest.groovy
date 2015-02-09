package org.scoverage

import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Test

class SeparateTestsAcceptanceTest extends AcceptanceTestUtils {

    private void testSeparate(boolean useAnt) throws Exception {
        File projectDir = new File('src/test/separate-tests')
        File subprojectDir = new File(projectDir, 'a')
        File testsSubprojectDir = new File(projectDir, 'a-tests')

        def build = setupBuild(projectDir, useAnt)
        build.forTasks('clean', 'reportScoverage').run()

        // ensure report is generated in base project ...
        def indexHtml = new File(reportDir(subprojectDir), 'index.html')
        checkFile('an index HTML file', indexHtml, true)

        // ... but not in test project ...
        def testsIndexHtml = new File(reportDir(testsSubprojectDir), 'index.html')
        checkFile('an index HTML file', testsIndexHtml, false)

        // ... and both statement and branch coverage is 100%
        def branchCoverage = coverage(reportDir(subprojectDir), CoverageType.Branch)
        def statementCoverage = coverage(reportDir(subprojectDir), CoverageType.Statement)
        Assert.assertThat('Branch coverage should be 100%, was ' + branchCoverage, branchCoverage, Is.is(100.0))
        Assert.assertThat('Statement coverage should be 100%, was ' + statementCoverage, statementCoverage, Is.is(100.0))
    }

    @Test
    public void testSeparateTestsWithAnt() throws Exception {
        testSeparate(true)
    }

    @Test
    public void testSeparateTestsWithZinc() throws Exception {
        testSeparate(false)
    }
}
