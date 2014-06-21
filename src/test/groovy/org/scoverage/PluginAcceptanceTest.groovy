package org.scoverage

import org.gradle.tooling.GradleConnector
import org.junit.Test

import static org.junit.Assert.assertThat
import static org.hamcrest.core.Is.is

class PluginAcceptanceTest {

    @Test
    public void testProjectWithCompleteCoverage() throws Exception {
        def build = GradleConnector.
                            newConnector().
                            forProjectDirectory(new File("src/test/happyday")).
                            connect().newBuild()
        build.forTasks('clean', 'checkScoverage').run()

        def html = new File('src/test/happyday/build/reports/scoverage/index.html')
        assertThat('an HTML file should be created at ' + html.absolutePath, html.exists(), is(true))
    }
}
