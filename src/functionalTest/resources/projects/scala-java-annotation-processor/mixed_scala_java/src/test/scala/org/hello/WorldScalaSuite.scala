package org.hello

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class WorldScalaSuite extends FunSuite {

  test("foo") {
    new WorldScala().foo()
  }
}
