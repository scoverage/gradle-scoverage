package org.scoverage;

import org.junit.Test;

public class ConfigurationCacheMultiModuleTest extends ScoverageFunctionalTest {

    public ConfigurationCacheMultiModuleTest() {
        super("scala-multi-module");
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
        secondRun.assertTaskSucceeded("a:" + ScoveragePlugin.getCHECK_NAME());
    }
}
