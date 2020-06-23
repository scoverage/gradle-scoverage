package org.composite.proj2

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ReporterSuite extends FunSuite {

  test("report"){

    val report = new Reporter().report("x")

    assertResult(Report(1, 2))(report)
  }
}
