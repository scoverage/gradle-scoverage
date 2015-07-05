package org.scoverage

import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.hamcrest.core.Is
import org.junit.Assert

/**
 * Some utils for easy acceptance testing.
 */
class AcceptanceTestUtils {


    protected BuildLauncher setupBuild(File projectRoot, boolean useAnt) {
        return GradleConnector.
            newConnector().
            forProjectDirectory(projectRoot).
            connect().
            newBuild().
            withArguments("-PuseAnt=$useAnt")
    }

    protected void checkFile(String description, File file, boolean shouldExist) throws Exception {
        Assert.assertThat(description + ' should be created at ' + file.absolutePath, file.exists(), Is.is(shouldExist))
    }

    protected File reportDir(File baseDir) {
        return new File(baseDir, 'build/reports/scoverage')
    }

}
