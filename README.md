gradle-scct
===========
A plugin to enable the use of SCCT in a gradle Scala project.

Getting started
---------------
```groovy
buildscript {
    repositories {
        maven { url 'http://mtkopone.github.com/scct/maven-repo' }
        maven { url 'http://maiflai.github.com/maven-repo' }
    }
    dependencies {
        classpath 'com.github.maiflai:gradle-scct:0.1-SNAPSHOT'
    }
}

apply plugin: 'scct'

dependencies {
    scct 'reaktor:scct_2.9.2:0.2-SNAPSHOT', 'org.scala-lang:scala-library:2.9.2'
}
```

This creates an additional task testCoverage which will run tests against instrumented code

- [x] instrumenting main scala code
- [x] running JUnit tests against instrumented scala code
- [ ] failing the build on lack of coverage

