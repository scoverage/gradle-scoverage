package org.scoverage;

import scoverage.Coverage;
import scoverage.report.CoverageAggregator;

import java.io.File;

public class AggregateReportApp {

    public static void main(String... args) {
        File rootDir = new File(args[0]);
        File reportDir = new File(args[1]);
        Boolean clean = Boolean.parseBoolean(args[2]);

        Boolean coverageOutputCobertura = java.lang.Boolean.valueOf(args[3]);
        Boolean coverageOutputXML = java.lang.Boolean.valueOf(args[4]);
        Boolean coverageOutputHTML = java.lang.Boolean.valueOf(args[5]);
        Boolean coverageDebug = java.lang.Boolean.valueOf(args[6]);

        Coverage coverage = CoverageAggregator.aggregate(rootDir, clean).get();

        ScoverageWriter.write(
            rootDir,
            reportDir,
            coverage,
            coverageOutputCobertura,
            coverageOutputXML,
            coverageOutputHTML,
            coverageDebug
        );
    }

}