package whales

import org.junit.Test
import org.junit.Assert

class BlueWhaleTest {

  @Test def bob(): Unit = {
    Assert.assertEquals("Whale cannot swim :(", new BlueWhale().swim(), "I'm swimming!")
  }
}