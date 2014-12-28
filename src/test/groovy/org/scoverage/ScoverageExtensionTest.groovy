package org.scoverage

import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.MatcherAssert.assertThat

class ScoverageExtensionTest {

    static def quote(String s) { '\'' + s + '\'' }

    static def doubleQuote(String s) { '"' + s + '"' }

    @Test
    public void testStringEscaping() throws Exception {
        def parameter = 'my param'
        assertThat(ScoverageExtension.escape(parameter), equalTo(doubleQuote(parameter)))
        assertThat(ScoverageExtension.escape(quote(parameter)), equalTo(quote(parameter)))
        assertThat(ScoverageExtension.escape(doubleQuote(parameter)), equalTo(doubleQuote(parameter)))
    }
}
