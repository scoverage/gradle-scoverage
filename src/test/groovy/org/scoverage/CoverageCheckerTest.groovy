package org.scoverage

import org.gradle.api.GradleException
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.function.Executable
import org.junit.rules.TemporaryFolder
import org.slf4j.LoggerFactory

import java.nio.file.Paths
import java.text.NumberFormat

import static org.junit.Assert.assertThat
import static org.junit.jupiter.api.Assertions.assertThrows

class CoverageCheckerTest {

    private NumberFormat numberFormat = NumberFormat.getInstance(Locale.US)

    private File reportDir = Paths.get(getClass().getClassLoader().getResource("checkTask").toURI()).toFile()

    private CoverageChecker checker = new CoverageChecker(LoggerFactory.getLogger(CoverageCheckerTest.class))

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder()

    // error when report file is not there

    @Test
    void failsWhenReportFileIsNotFound() {
        assertFailure(CoverageChecker.fileNotFoundErrorMsg(CoverageType.Line), {
            checker.checkLineCoverage(tempDir.getRoot(), CoverageType.Line, 0.0, numberFormat)
        })
    }

    // line coverage

    @Test
    void failsWhenLineRateIsBelowTarget() {
        assertFailure(CoverageChecker.errorMsg("66", "100", CoverageType.Line), {
            checker.checkLineCoverage(reportDir, CoverageType.Line, 1.0, numberFormat)
        })
    }

    @Test
    void doesNotFailWhenLineRateIsAtTarget() {
        checker.checkLineCoverage(reportDir, CoverageType.Line, 0.66, numberFormat)
    }

    @Test
    void doesNotFailWhenLineRateIsAboveTarget() {
        checker.checkLineCoverage(reportDir, CoverageType.Line, 0.6, numberFormat)
    }

    // Statement coverage

    @Test
    void failsWhenStatementRateIsBelowTarget() {
        assertFailure(CoverageChecker.errorMsg(numberFormat.format(new Double(33.33)), "100", CoverageType.Statement), {
            checker.checkLineCoverage(reportDir, CoverageType.Statement, 1.0, numberFormat)
        })
    }

    @Test
    void doesNotFailWhenStatementRateIsAtTarget() {
        checker.checkLineCoverage(reportDir, CoverageType.Statement, 0.33, numberFormat)
    }

    @Test
    void doesNotFailWhenStatementRateIsAboveTarget() {
        checker.checkLineCoverage(reportDir, CoverageType.Statement, 0.3, numberFormat)
    }

    // Branch coverage

    @Test
    void failsWhenBranchRateIsBelowTarget() {
        assertFailure(CoverageChecker.errorMsg("50", "100", CoverageType.Branch), {
            checker.checkLineCoverage(reportDir, CoverageType.Branch, 1.0, numberFormat)
        })
    }

    @Test
    void doesNotFailWhenBranchRateIsAtTarget() {
        checker.checkLineCoverage(reportDir, CoverageType.Branch, 0.5, numberFormat)
    }

    @Test
    void doesNotFailWhenBranchRateIsAboveTarget() {
        checker.checkLineCoverage(reportDir, CoverageType.Branch, 0.45, numberFormat)
    }

    private void assertFailure(String message, Executable executable) {
        GradleException e = assertThrows(GradleException.class, executable)
        assertThat(e, new CauseMatcher(message))
    }
}

/**
 * Copied from the Internet, just to check if we have correct exception thrown.
 */
class CauseMatcher extends TypeSafeMatcher<GradleException> {

    private final String expectedMessage

    CauseMatcher(String expectedMessage) {
        this.expectedMessage = expectedMessage
    }

    @Override
    protected boolean matchesSafely(GradleException item) {
        return item.getMessage().contains(expectedMessage)
    }

    @Override
    void describeTo(Description description) {
        description.appendText("expects message ")
                .appendValue(expectedMessage)
    }
}