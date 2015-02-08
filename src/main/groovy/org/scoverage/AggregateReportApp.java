package org.scoverage;

import scoverage.Coverage;
import scoverage.report.CoberturaXmlWriter;
import scoverage.report.CoverageAggregator;
import scoverage.report.ScoverageHtmlWriter;

import java.io.File;

public class AggregateReportApp {

    public static void main(String... args) {
        File rootDir = new File(args[0]);
        File reportDir = new File(args[1]);
        Boolean clean = Boolean.parseBoolean(args[2]);
        reportDir.mkdirs();
        Coverage coverage = CoverageAggregator.aggregate(rootDir, clean).get();
        new ScoverageHtmlWriter(rootDir, reportDir).write(coverage);
        new CoberturaXmlWriter(rootDir, reportDir).write(coverage);
    }

}