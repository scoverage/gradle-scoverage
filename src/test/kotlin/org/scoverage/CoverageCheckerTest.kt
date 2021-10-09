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
import java.util.*

import org.junit.Assert.assertThat
import org.junit.jupiter.api.Assertions.assertThrows

class CoverageCheckerTest {

    private val numberFormat = NumberFormat.getInstance(Locale.US)

    private val reportDir = Paths.get(javaClass.getClassLoader().getResource("checkTask").toURI()).toFile()

    private val checker = CoverageChecker(LoggerFactory.getLogger(CoverageCheckerTest::class.java))

    @get:Rule
    val tempDir = TemporaryFolder()

    // error when report file is not there

    @Test
    fun failsWhenReportFileIsNotFound() {
        assertFailure(CoverageChecker.fileNotFoundErrorMsg(CoverageType.Line), {
            checker.checkLineCoverage(tempDir.getRoot(), CoverageType.Line, 0.0, numberFormat)
        })
    }

    // line coverage

    @Test
    fun failsWhenLineRateIsBelowTarget() {
        assertFailure(CoverageChecker.errorMsg("66", "100", CoverageType.Line), {
            checker.checkLineCoverage(reportDir, CoverageType.Line, 1.0, numberFormat)
        })
    }

    @Test
    fun doesNotFailWhenLineRateIsAtTarget() {
        checker.checkLineCoverage(reportDir, CoverageType.Line, 0.66, numberFormat)
    }

    @Test
    fun doesNotFailWhenLineRateIsAboveTarget() {
        checker.checkLineCoverage(reportDir, CoverageType.Line, 0.6, numberFormat)
    }

    // Statement coverage

    @Test
    fun failsWhenStatementRateIsBelowTarget() {
        assertFailure(CoverageChecker.errorMsg(numberFormat.format(33.33), "100", CoverageType.Statement), {
            checker.checkLineCoverage(reportDir, CoverageType.Statement, 1.0, numberFormat)
        })
    }

    @Test
    fun doesNotFailWhenStatementRateIsAtTarget() {
        checker.checkLineCoverage(reportDir, CoverageType.Statement, 0.33, numberFormat)
    }

    @Test
    fun doesNotFailWhenStatementRateIsAboveTarget() {
        checker.checkLineCoverage(reportDir, CoverageType.Statement, 0.3, numberFormat)
    }

    // Branch coverage

    @Test
    fun failsWhenBranchRateIsBelowTarget() {
        assertFailure(CoverageChecker.errorMsg("50", "100", CoverageType.Branch), {
            checker.checkLineCoverage(reportDir, CoverageType.Branch, 1.0, numberFormat)
        })
    }

    @Test
    fun doesNotFailWhenBranchRateIsAtTarget() {
        checker.checkLineCoverage(reportDir, CoverageType.Branch, 0.5, numberFormat)
    }

    @Test
    fun doesNotFailWhenBranchRateIsAboveTarget() {
        checker.checkLineCoverage(reportDir, CoverageType.Branch, 0.45, numberFormat)
    }

    private fun assertFailure(message: String, executable: Executable) {
        val e: GradleException = assertThrows(GradleException::class.java, executable)
        assertThat(e, CauseMatcher(message))
    }
}

/**
 * Copied from the Internet, just to check if we have correct exception thrown.
 */
class CauseMatcher(private val expectedMessage: String): TypeSafeMatcher<GradleException>() {

    @Override
    protected override fun matchesSafely(item: GradleException): Boolean {
        return item.message?.contains(expectedMessage) == true
    }

    override fun describeTo(description: Description) {
        description.appendText("expects message ")
                .appendValue(expectedMessage)
    }
}
