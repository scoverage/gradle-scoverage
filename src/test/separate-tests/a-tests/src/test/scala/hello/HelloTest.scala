package hello

import org.junit.Test
import org.junit.Assert.assertEquals

class HelloTest {

  @Test def testText() {
    assertEquals("Hello World", new Hello().text)
  }

}
