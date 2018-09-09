package org.scoverage

import org.gradle.api.Plugin
import org.gradle.api.Project

class ScoveragePlugin implements Plugin<Project> {
    static String CONFIGURATION_NAME = 'scoverage'

    static String TEST_NAME = 'testScoverage'
    static String REPORT_NAME = 'reportScoverage'
    static String CHECK_NAME = 'checkScoverage'
    static String COMPILE_NAME = 'compileScoverageScala'

    @Override
    void apply(Project t) {
        if (t.extensions.findByName(CONFIGURATION_NAME) == null) {
            t.extensions.create(CONFIGURATION_NAME, ScoverageExtension, t)
        }
    }

    protected static ScoverageExtension extensionIn(Project project) {
        project.extensions[CONFIGURATION_NAME] as ScoverageExtension
    }
}
