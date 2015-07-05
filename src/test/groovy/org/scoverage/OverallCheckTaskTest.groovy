package org.scoverage

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

/**
 * Copied from the Internet, just to check if we have correct exception thrown.
 */
class CauseMatcher extends TypeSafeMatcher<Throwable> {

    private final Class<? extends Throwable> type;
    private final String expectedMessage;

    public CauseMatcher(Class<? extends Throwable> type, String expectedMessage) {
        this.type = type;
        this.expectedMessage = expectedMessage;
    }

    @Override
    protected boolean matchesSafely(Throwable item) {
        return item.getClass().isAssignableFrom(type) && item.getMessage().contains(expectedMessage);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("expects type ")
                .appendValue(type)
                .appendText(" and a message ")
                .appendValue(expectedMessage);
    }
}

class OverallCheckTaskTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none()

    private Project projectForRate(Number coverageRate, CoverageType type) {
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply(ScoveragePlugin)
        project.tasks.create('bob', OverallCheckTask) {
            minimumRate = coverageRate
            reportDir = new File('src/test/resources')
            coverageType = type
        }
        project
    }

    // error when report file is not there

    @Test
    void failsWhenReportFileIsNotFound() {
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply(ScoveragePlugin)
        project.tasks.create('bob', OverallCheckTask) {
            minimumRate = 1.0
            reportDir = new File('src/test/nothingthere')
            coverageType = CoverageType.Line
        }
        expectedException.expectCause(new CauseMatcher(
                GradleException.class,
                OverallCheckTask.fileNotFoundErrorMsg(CoverageType.Line)
        ))
        project.tasks.bob.execute()
    }

    // line coverage

    @Test
    void failsWhenLineRateIsBelowTarget() {
        Project project = projectForRate(1, CoverageType.Line)
        expectedException.expectCause(new CauseMatcher(
                GradleException.class,
                OverallCheckTask.errorMsg("66", "100", CoverageType.Line)
        ))
        project.tasks.bob.execute()
    }

    @Test
    void doesNotFailWhenLineRateIsAtTarget() throws Exception {
        Project project = projectForRate(0.66, CoverageType.Line)
        project.tasks.bob.execute()
    }

    @Test
    void doesNotFailWhenLineRateIsAboveTarget() throws Exception {
        Project project = projectForRate(0.6, CoverageType.Line)
        project.tasks.bob.execute()
    }

    // Statement coverage

    @Test
    void failsWhenStatementRateIsBelowTarget() {
        Project project = projectForRate(1, CoverageType.Statement)
        expectedException.expectCause(new CauseMatcher(
                GradleException.class,
                OverallCheckTask.errorMsg("33.33", "100", CoverageType.Statement)
        ))
        project.tasks.bob.execute()
    }

    @Test
    void doesNotFailWhenStatementRateIsAtTarget() throws Exception {
        Project project = projectForRate(0.33, CoverageType.Statement)
        project.tasks.bob.execute()
    }

    @Test
    void doesNotFailWhenStatementRateIsAboveTarget() throws Exception {
        Project project = projectForRate(0.3, CoverageType.Statement)
        project.tasks.bob.execute()
    }

    // Branch coverage

    @Test
    void failsWhenBranchRateIsBelowTarget() {
        Project project = projectForRate(1, CoverageType.Branch)
        expectedException.expectCause(new CauseMatcher(
                GradleException.class,
                OverallCheckTask.errorMsg("50", "100", CoverageType.Branch)
        ))
        project.tasks.bob.execute()
    }

    @Test
    void doesNotFailWhenBranchRateIsAtTarget() throws Exception {
        Project project = projectForRate(0.50, CoverageType.Branch)
        project.tasks.bob.execute()
    }

    @Test
    void doesNotFailWhenBranchRateIsAboveTarget() throws Exception {
        Project project = projectForRate(0.45, CoverageType.Branch)
        project.tasks.bob.execute()
    }

}
