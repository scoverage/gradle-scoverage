package org.hello.a

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class WorldASuite extends FunSuite {

  test("fooA") {
    new WorldA().fooA()
  }
}
