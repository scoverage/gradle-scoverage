package org.scoverage

import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

import java.nio.file.Files

import static groovy.io.FileType.FILES

@UntrackedTask(because = "Removes identical class files from the instrumented output in place")
abstract class PruneIdenticalScoverageClassesTask extends DefaultTask {

    @Internal
    abstract DirectoryProperty getOriginalClassesDirectory()

    @Internal
    abstract DirectoryProperty getInstrumentedClassesDirectory()

    @TaskAction
    void prune() {
        if (!originalClassesDirectory.present || !instrumentedClassesDirectory.present) {
            return
        }

        def originalDestinationDir = originalClassesDirectory.get().asFile
        def destinationDir = instrumentedClassesDirectory.get().asFile
        if (!originalDestinationDir.exists() || !destinationDir.exists()) {
            return
        }

        logger.info("Deleting classes compiled by scoverage but non-instrumented (identical to normal compilation)")

        def findFiles = { File dir, Closure<Boolean> condition = null ->
            def files = []

            if (dir.exists()) {
                dir.eachFileRecurse(FILES) { f ->
                    if (condition == null || condition(f)) {
                        def relativePath = dir.relativePath(f)
                        files << relativePath
                    }
                }
            }

            files
        }

        def isSameFile = { String relativePath ->
            def fileA = new File(originalDestinationDir, relativePath)
            def fileB = new File(destinationDir, relativePath)
            FileUtils.contentEquals(fileA, fileB)
        }

        def originalClasses = findFiles(originalDestinationDir)
        def identicalInstrumentedClasses = findFiles(destinationDir, { f ->
            def relativePath = destinationDir.relativePath(f)
            originalClasses.contains(relativePath) && isSameFile(relativePath)
        })

        identicalInstrumentedClasses.each { f ->
            Files.deleteIfExists(destinationDir.toPath().resolve(f))
        }
    }
}
