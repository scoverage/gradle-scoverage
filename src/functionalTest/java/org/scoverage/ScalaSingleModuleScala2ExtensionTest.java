package org.scoverage;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ScalaSingleModuleScala2ExtensionTest extends ScalaSingleModuleScalaExtensionTest {

    @Override
    protected List<String> getVersionAgruments() {
        return ScalaVersionArguments.version2;
    }

}
