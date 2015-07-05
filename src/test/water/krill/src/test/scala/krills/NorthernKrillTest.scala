package krills

import org.junit.Test
import org.junit.Assert

class NorthernKrillTest {

  @Test def bob(): Unit = {
    Assert.assertEquals("Krill can swim", new NorthernKrill().swim(), "I can only float :(")
  }
}