package org.hello

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class World213Suite extends FunSuite {

  test("foo") {
    new World213().foo()
  }
}