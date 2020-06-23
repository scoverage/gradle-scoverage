package org.composite.proj2

class Reporter {

  def report(rawData: String): Report = {
    Report(1,2)
  }

  class InnerReporter {

    def lala(): Unit = {

      val x = 1 + 1
      x
    }
  }
}

case class Report(id: Long, count: Int)