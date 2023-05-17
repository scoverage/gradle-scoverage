package org.scoverage

class ScalaVersion {
    final String primaryVersion
    final Optional<String> secondaryVersion
    final Integer majorVersion
    final String scalacScoverageVersion
    final String scalacScoveragePluginVersion
    final String scalacScoverageRuntimeVersion

    ScalaVersion(primaryVersion) {
        this(primaryVersion, Optional.empty())
    }

    ScalaVersion(String primaryVersion, Optional<String> secondaryVersion) {
        this.primaryVersion = primaryVersion
        this.secondaryVersion = secondaryVersion

        this.majorVersion = primaryVersion.substring(0, primaryVersion.indexOf('.')).toInteger()
        this.scalacScoverageVersion = this.majorVersion < 3
                ? primaryVersion.substring(0, primaryVersion.lastIndexOf('.'))
                : this.majorVersion.toString()
        this.scalacScoveragePluginVersion = secondaryVersion.orElse(primaryVersion)
        this.scalacScoverageRuntimeVersion = scalacScoveragePluginVersion.substring(0, scalacScoveragePluginVersion.lastIndexOf('.'))
    }

    @Override
    String toString() {
        return majorVersion < 3 ? primaryVersion : "$primaryVersion (${secondaryVersion.get()})"
    }
}
