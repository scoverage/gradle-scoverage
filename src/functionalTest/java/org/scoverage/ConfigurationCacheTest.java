package org.scoverage;

import org.junit.Test;

import java.util.List;

public class ConfigurationCacheTest extends ScoverageFunctionalTest {

    public ConfigurationCacheTest() {
        super("scala-single-module");
    }

    @Override
    protected List<String> getVersionAgruments() {
        return ScalaVersionArguments.version2;
    }

    @Test
    public void checkScoverageStoresAndReusesConfigurationCache() throws Exception {

        cleanProjectForConfigurationCache();

        prepareConfigurationCacheRun(ScoveragePlugin.getCHECK_NAME());
        AssertableBuildResult firstRun = buildPreparedConfigurationCacheRun();
        firstRun.assertConfigurationCacheStored();

        prepareConfigurationCacheRun(ScoveragePlugin.getCHECK_NAME());
        AssertableBuildResult secondRun = buildPreparedConfigurationCacheRun();

        secondRun.assertConfigurationCacheReused();
        secondRun.assertTaskSucceeded(ScoveragePlugin.getCHECK_NAME());
        assertCoverage(50.0);
    }
}
