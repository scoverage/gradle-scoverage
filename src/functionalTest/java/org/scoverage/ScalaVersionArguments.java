package org.scoverage;

import java.util.Arrays;
import java.util.List;

public interface ScalaVersionArguments {
    List<String> version2WithLegacyScalatest = Arrays.asList(
            "-PscalaVersionMajor=2",
            "-PscalaVersionMinor=13",
            "-PscalaVersionBuild=14",
            "-PjunitVersion=5.3.2",
            "-PjunitPlatformVersion=1.3.2",
            "-PscalatestVersion=3.0.8"
    );

    List<String> version2 = Arrays.asList(
            "-PscalaVersionMajor=2",
            "-PscalaVersionMinor=13",
            "-PscalaVersionBuild=14",
            "-PjunitVersion=5.3.2",
            "-PjunitPlatformVersion=1.3.2",
            "-PscalatestVersion=3.2.16"
    );

    List<String> version3 = Arrays.asList(
            "-PscalaVersionMajor=3",
            "-PscalaVersionMinor=4",
            "-PscalaVersionBuild=2",
            "-PjunitVersion=5.3.2",
            "-PjunitPlatformVersion=1.3.2",
            "-PscalatestVersion=3.2.16"
    );
}
