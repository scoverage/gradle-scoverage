package org.hello

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class World3_2Suite extends FunSuite {

  test("foo") {
    new World3_2().foo()
  }
}