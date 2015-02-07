package hello

import org.junit.Test

class ResourcesTest {
  @Test
  def mainResourcesAreBuilt() {
    assert(getClass.getResource("/main.txt") != null)
  }
  @Test
  def testResourcesAreBuilt() {
    assert(getClass.getResource("/test.txt") != null)
  }
}