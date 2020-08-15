package org.scoverage;

import org.junit.Ignore;

/**
 * Tests is currently ignored as support for Scala 2.13 is not available yet.
 *
 * @see <a href="https://github.com/scoverage/gradle-scoverage/issues/106">Issue #106</a>.
 */
@Ignore
public class Scala213Test extends ScalaVersionTest {
    public Scala213Test() {
        super("2_13");
    }
}