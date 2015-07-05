package org.scoverage

import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.GradleConnector
import org.hamcrest.core.Is
import org.junit.Assert

/**
 * Some utils for easy acceptance testing.
 */
class AcceptanceTestUtils {

    XmlParser parser

    AcceptanceTestUtils() {
        parser = new XmlParser()
        parser.setFeature('http://apache.org/xml/features/disallow-doctype-decl', false)
        parser.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)
    }

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

    protected Double coverage(File reportDir, CoverageType coverageType) {
        File reportFile = new File(reportDir, coverageType.fileName)
        def xml = parser.parse(reportFile)
        xml.attribute(coverageType.paramName).toDouble()
    }
}
