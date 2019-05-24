package org.hello

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class WorldIntSuite extends FunSuite {

  test("bar") {
    new World().bar()
  }
}
