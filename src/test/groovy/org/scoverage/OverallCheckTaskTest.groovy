package org.scoverage

import org.gradle.api.GradleException
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Test

import java.text.NumberFormat

import static org.junit.Assert.assertNull
import static org.junit.Assert.assertThat
import static org.scoverage.OverallCheckTask.checkLineCoverage

/**
 * Copied from the Internet, just to check if we have correct exception thrown.
 */
class CauseMatcher extends TypeSafeMatcher<Throwable> {

    private final Class<? extends Throwable> type
    private final String expectedMessage

    CauseMatcher(Class<? extends Throwable> type, String expectedMessage) {
        this.type = type
        this.expectedMessage = expectedMessage
    }

    @Override
    protected boolean matchesSafely(Throwable item) {
        return item.getClass().isAssignableFrom(type) && item.getMessage().contains(expectedMessage)
    }

    @Override
    void describeTo(Description description) {
        description.appendText("expects type ")
                .appendValue(type)
                .appendText(" and a message ")
                .appendValue(expectedMessage)
    }
}

class OverallCheckTaskTest {

    private NumberFormat numberFormat = NumberFormat.getInstance(Locale.US)

    private static File reportDir = new File('src/test/resources')

    private static Matcher<Throwable> failsWith(String message) {
        return new CauseMatcher(
                GradleException.class,
                message
        )
    }

    // error when report file is not there

    @Test
    void failsWhenReportFileIsNotFound() {
        assertThat(
                checkLineCoverage(numberFormat, new File('src/test/nothingthere'), CoverageType.Line, 0.0),
                failsWith(OverallCheckTask.fileNotFoundErrorMsg(CoverageType.Line)))
    }

    // line coverage

    @Test
    void failsWhenLineRateIsBelowTarget() {
        assertThat(
                checkLineCoverage(numberFormat, reportDir, CoverageType.Line, 1.0),
                failsWith(OverallCheckTask.errorMsg("66", "100", CoverageType.Line)))
    }

    @Test
    void doesNotFailWhenLineRateIsAtTarget() {
        assertNull(checkLineCoverage(numberFormat, reportDir, CoverageType.Line, 0.66))
    }

    @Test
    void doesNotFailWhenLineRateIsAboveTarget() {
        assertNull(checkLineCoverage(numberFormat, reportDir, CoverageType.Line, 0.6))
    }

    // Statement coverage

    @Test
    void failsWhenStatementRateIsBelowTarget() {
        assertThat(
                checkLineCoverage(numberFormat, reportDir, CoverageType.Statement, 1.0),
                failsWith(OverallCheckTask.errorMsg(numberFormat.format(new Double(33.33)), "100", CoverageType.Statement)))
    }

    @Test
    void doesNotFailWhenStatementRateIsAtTarget() {
        assertNull(checkLineCoverage(numberFormat, reportDir, CoverageType.Statement, 0.33))
    }

    @Test
    void doesNotFailWhenStatementRateIsAboveTarget() {
        assertNull(checkLineCoverage(numberFormat, reportDir, CoverageType.Statement, 0.3))
    }

    // Branch coverage

    @Test
    void failsWhenBranchRateIsBelowTarget() {
        assertThat(
                checkLineCoverage(numberFormat, reportDir, CoverageType.Branch, 1.0),
                failsWith(OverallCheckTask.errorMsg("50", "100", CoverageType.Branch)))
    }

    @Test
    void doesNotFailWhenBranchRateIsAtTarget() {
        assertNull(checkLineCoverage(numberFormat, reportDir, CoverageType.Branch, 0.5))
    }

    @Test
    void doesNotFailWhenBranchRateIsAboveTarget() {
        assertNull(checkLineCoverage(numberFormat, reportDir, CoverageType.Branch, 0.45))
    }

}
