/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.monitoring.cleaner.local;

import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.InternalSettingsPlugin;
import org.elasticsearch.xpack.monitoring.cleaner.AbstractIndicesCleanerTestCase;
import org.elasticsearch.xpack.monitoring.exporter.local.LocalExporter;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collection;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.equalTo;

public class LocalIndicesCleanerTests extends AbstractIndicesCleanerTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        ArrayList<Class<? extends Plugin>> plugins = new ArrayList<>(super.nodePlugins());
        plugins.add(InternalSettingsPlugin.class);
        return plugins;
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put("xpack.monitoring.exporters._local.type", LocalExporter.TYPE)
                .build();
    }

    @Override
    protected void createIndex(String name, DateTime creationDate) {
        assertAcked(prepareCreate(name)
                .setSettings(Settings.builder().put(IndexMetaData.SETTING_CREATION_DATE, creationDate.getMillis()).build()));
        ensureYellow(name);
    }

    @Override
    protected void assertIndicesCount(int count) throws Exception {
        assertBusy(() -> {
            //we set ignore_unavailable to true for this request as the monitoring index gets deleted concurrently with this assertion
            //in some cases. When the plugin security is enabled, it expands wildcards to the existing index, which then gets deleted,
            //so when es core gets the request with the explicit index name, it throws an index not found exception as that index
            //doesn't exist anymore. If we ignore unavailable instead no error will be thrown.
            GetSettingsResponse getSettingsResponse = client().admin().indices().prepareGetSettings()
                    .setIndicesOptions(IndicesOptions.fromOptions(true, true, true, true)).get();
            assertThat(getSettingsResponse.getIndexToSettings().size(), equalTo(count));
        });
    }
}
