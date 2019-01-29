package org.scoverage

import org.gradle.api.file.FileCollection

import java.lang.reflect.Method

class ScoverageRunner {

    private FileCollection runtimeClasspath

    ScoverageRunner(FileCollection runtimeClasspath) {

        this.runtimeClasspath = runtimeClasspath
    }

    def run(Closure<?> action) {

        URLClassLoader cloader = (URLClassLoader) Thread.currentThread().getContextClassLoader()

        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class)
        method.setAccessible(true)

        runtimeClasspath.files.each { f ->
            def url = f.toURL()
            if (!cloader.getURLs().contains(url)) {
                method.invoke(cloader, url)
            }
        }

        action.call()
    }
}
