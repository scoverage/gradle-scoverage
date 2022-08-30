package org.scoverage

import org.gradle.api.Action
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor

class ScoverageRunner {

    private final WorkerExecutor workerExecutor

    @Classpath
    final FileCollection runtimeClasspath

    ScoverageRunner(WorkerExecutor workerExecutor, FileCollection runtimeClasspath) {

        this.workerExecutor = workerExecutor
        this.runtimeClasspath = runtimeClasspath
    }

    /**
     * The runner makes use of Gradle's worker API to run tasks with scoverage's classpath without affecting the main
     * gradle classloader/classpath.
     *
     * @see <a href="https://docs.gradle.org/current/userguide/custom_tasks.html#worker_api">Worker API guide</a>
     * @see <a href="https://github.com/gradle/guides/issues/295">Worker API guide issue<a/>
     */
    def <T extends WorkParameters> void run(Class<? extends WorkAction<T>> workActionClass, Action<? super T> parameterAction) {

        def queue    = workerExecutor.classLoaderIsolation() {spec ->
            spec.getClasspath().from(runtimeClasspath)
        }
        queue.submit(workActionClass, parameterAction)
    }
}
