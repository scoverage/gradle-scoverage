package org.hello

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class World2_12Suite extends FunSuite {

  test("foo") {
    new World2_12().foo()
  }
}