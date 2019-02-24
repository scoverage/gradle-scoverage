package org.hello.common

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class WorldCommonSuite extends FunSuite {

  test("fooCommon") {
    new WorldCommon().fooCommon()
  }
}