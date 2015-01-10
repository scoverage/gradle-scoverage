package org.scoverage

import org.gradle.tooling.GradleConnector
import org.junit.Test

import static org.junit.Assert.assertThat
import static org.hamcrest.core.Is.is

class PluginAcceptanceTest {

    static def checkHappyDay(boolean useAnt) {
        def projectRoot = "src/test/happy day"
        def build = GradleConnector.
                newConnector().
                forProjectDirectory(new File(projectRoot)).
                connect().newBuild().
                withArguments("-PuseAnt=$useAnt")
        build.forTasks('clean', 'checkScoverage').run()

        def html = new File("$projectRoot/build/reports/scoverage/index.html")
        assertThat('an HTML file should be created at ' + html.absolutePath, html.exists(), is(true))
    }

    @Test
    public void testAntProjectWithCompleteCoverage() throws Exception {
        checkHappyDay(true)
    }

    @Test
    public void testZincProjectWithCompleteCoverage() throws Exception {
        checkHappyDay(false)
    }
}
