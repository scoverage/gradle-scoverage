package org.hello.b

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class WorldBSuite extends FunSuite {

  test("fooB") {
    new WorldB().fooB()
  }
}
