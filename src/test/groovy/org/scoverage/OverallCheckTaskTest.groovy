package org.scoverage

import org.gradle.api.Project
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class OverallCheckTaskTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none()

    private Project projectForLineRate(Number lineRate) {
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply(ScoveragePlugin)
        project.tasks.create('bob', OverallCheckTask) {
            minimumLineRate = lineRate
            cobertura = new File('src/test/resources/cobertura.xml')
        }
        project
    }

    @Test
    void failsWhenLineRateIsBelowTarget(){
        Project project = projectForLineRate(1)
        expectedException.expect(TaskExecutionException)
        project.tasks.bob.execute()
    }

    @Test
    void doesNotFailWhenLineRateIsAtTarget() throws Exception {
        Project project = projectForLineRate(0.66)
        project.tasks.bob.execute()
    }

    @Test
    void doesNotFailWhenLineRateIsAboveTarget() throws Exception {
        Project project = projectForLineRate(0.6)
        project.tasks.bob.execute()
    }

}
