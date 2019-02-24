package org.hello

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class WorldScalaOnlySuite extends FunSuite {

  test("foo") {
    new WorldScalaOnly().foo()
  }
}