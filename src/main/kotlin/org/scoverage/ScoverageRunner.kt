package org.scoverage

import groovy.lang.Closure
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import java.net.URL
import java.net.URLClassLoader

class ScoverageRunner(@Classpath val runtimeClasspath: FileCollection) {

    fun run(action: Closure<*>) {

        val cloader = Thread.currentThread().getContextClassLoader() as URLClassLoader

        val method = URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java)
        method.isAccessible = true

        runtimeClasspath.files.forEach { f ->
            val url = f.toURI().toURL()
            if (!cloader.urLs.contains(url)) {
                method.invoke(cloader, url)
            }
        }

        action.call()
    }
}
