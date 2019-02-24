package org.hello

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class World212Suite extends FunSuite {

  test("foo") {
    new World212().foo()
  }
}