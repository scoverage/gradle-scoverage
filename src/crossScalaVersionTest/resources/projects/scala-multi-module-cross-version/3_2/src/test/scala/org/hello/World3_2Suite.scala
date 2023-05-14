package org.hello

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class World3_2Suite extends AnyFunSuite {

  test("foo") {
    new World3_2().foo()
  }
}