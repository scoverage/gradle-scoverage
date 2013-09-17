gradle-scct
===========
A plugin to enable the use of SCCT in a gradle Scala project.

This has now been deployed to maven central.

Getting started
---------------
```groovy
buildscript {
    repositories {
        mavenCentral()
        maven { url 'http://mtkopone.github.com/scct/maven-repo' }
    }
    dependencies {
        classpath 'com.github.maiflai:gradle-scct:0.3'
    }
}

apply plugin: 'scct'

dependencies {
    scct 'reaktor:scct_2.10:0.2-SNAPSHOT'
    compile 'org.scala-lang:scala-library:2.10.1'
}
```

This creates an additional task testCoverage which will run tests against instrumented code

- [x] instrumenting main scala code
- [x] running JUnit tests against instrumented scala code
- [x] failing the build on lack of coverage

