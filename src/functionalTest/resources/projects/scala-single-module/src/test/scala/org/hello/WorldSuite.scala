package org.hello

import org.junit.runner.RunWith
import org.scalatest.funsuite._
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class WorldSuite extends AnyFunSuite {

  test("foo") {
    new World().foo()
  }
}