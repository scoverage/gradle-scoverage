package com.github.maiflai

import org.gradle.api.Plugin
import org.gradle.api.Project

class ScctPlugin implements Plugin<Project> {

    static String CONFIGURATION_NAME = 'scct'

    static String TEST_NAME = 'testScct'
    static String CHECK_NAME = 'checkScct'
    static String COMPILE_NAME = 'compileScctScala'

    @Override
    void apply(Project t) {
        if (t.extensions.findByName(CONFIGURATION_NAME) == null) {
            t.extensions.create(CONFIGURATION_NAME, ScctExtension, t)
        }
    }

}
