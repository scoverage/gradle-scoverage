package org.composite.proj1

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FooSuite extends FunSuite {

  test("bar"){

    new Foo().bar()
  }
}