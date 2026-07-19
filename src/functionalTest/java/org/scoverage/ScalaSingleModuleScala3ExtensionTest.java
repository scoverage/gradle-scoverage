package org.scoverage;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ScalaSingleModuleScala3ExtensionTest extends ScalaSingleModuleScalaExtensionTest {

    @Override
    protected List<String> getVersionAgruments() {
        return ScalaVersionArguments.version3;
    }

}
