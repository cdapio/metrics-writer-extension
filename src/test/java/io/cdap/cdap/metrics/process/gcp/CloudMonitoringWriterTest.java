/*
 * Copyright © 2021 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.cdap.metrics.process.gcp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.cdap.cdap.api.metrics.MetricType;
import io.cdap.cdap.api.metrics.MetricValue;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Test for {@link CloudMonitoringWriter}
 */
public class CloudMonitoringWriterTest {

  private static final String TEST_CONFIG_PATH = "src/test/resources/metrics_writer_config.json";

  @Test
  public void testConfig() {
    MonitoringConfig monitoringConfig = new CloudMonitoringWriter().getConfig(TEST_CONFIG_PATH);
    Assert.assertEquals(8, monitoringConfig.getMetricsMapping().size());
    String cdapMetricName = "program.failed.runs";
    String gcpMetricName = "datafusion.googleapis.com/odf_pipeline/runs_completed_count";
    Assert.assertEquals(gcpMetricName,
                        monitoringConfig.getMetricsMapping().get(cdapMetricName).getMetricType());
    Assert.assertEquals(1,
                        monitoringConfig.getMetricsMapping().get(cdapMetricName).getMetricLabels().size());
    Assert.assertEquals(1,
                        monitoringConfig.getMetricsMapping().get(cdapMetricName).getResourceLabels().size());
    Assert.assertEquals(5,
                        monitoringConfig.getMetricsMapping().get(cdapMetricName).getAutoFillLabels().size());
  }

  @Test
  public void testFindMapping() {
    CloudMonitoringWriter metricsWriter = new CloudMonitoringWriter();
    MonitoringConfig monitoringConfig = metricsWriter.getConfig(TEST_CONFIG_PATH);
    Map<String, String> tags1 = ImmutableMap.of("prg", "Spark", "namespace", "system");
    MetricValue value = new MetricValue("program.completed.runs", MetricType.COUNTER, 2);

    // The tags do not have the specified prg type in config file, so should return empty
    // Config file has {"tag": "prg", "values": "DataPipelineWorkflow,DataStreamsSparkStreaming"}
    Optional<Map.Entry<String, MetricsMapping>> mappingEntry = metricsWriter.getMappingEntry(
      monitoringConfig.getMetricsMapping(), value, tags1);
    Assert.assertFalse(mappingEntry.isPresent());

    // Add a matching tag from config file and now the mapping entry should be present
    Map<String, String> tags2 = ImmutableMap.of("prg", "DataPipelineWorkflow", "namespace", "system");
    Optional<Map.Entry<String, MetricsMapping>> prgMappingEntry = metricsWriter.getMappingEntry(
      monitoringConfig.getMetricsMapping(), value, tags2);
    Assert.assertTrue(prgMappingEntry.isPresent());
  }

  @Test
  public void testTagMatch() {
    CloudMonitoringWriter metricsWriter = new CloudMonitoringWriter();
    MonitoringConfig monitoringConfig = metricsWriter.getConfig(TEST_CONFIG_PATH);
    Map<String, String> cdapTags1 = ImmutableMap.of("prg", "DataPipelineWorkflow");
    Map<String, String> cdapTags2 = ImmutableMap.of("prg", "DataStreamsSparkStreaming");
    Map<String, String> cdapTags3 = ImmutableMap.of("prg", "random");
    List<TagFilter> tagFilters = monitoringConfig.getMetricsMapping().get("program.completed.runs").getTagFilters();

    // add more filters
    List<TagFilter> additionalFilters = new ArrayList<>(tagFilters);
    additionalFilters.add(new TagFilter("test", "val1, val2"));
    Map<String, String> cdapTags4 = ImmutableMap.of("prg", "DataPipelineWorkflow", "test", "val1");
    Map<String, String> cdapTags5 = ImmutableMap.of("prg", "DataPipelineWorkflow", "test", "val3");

    // Match only when all tags are valid and value is present
    Assert.assertTrue(metricsWriter.allTagsMatch(tagFilters, cdapTags1));
    Assert.assertTrue(metricsWriter.allTagsMatch(tagFilters, cdapTags2));
    Assert.assertFalse(metricsWriter.allTagsMatch(tagFilters, cdapTags3));

    Assert.assertFalse(metricsWriter.allTagsMatch(additionalFilters, cdapTags1));
    Assert.assertTrue(metricsWriter.allTagsMatch(additionalFilters, cdapTags4));
    Assert.assertFalse(metricsWriter.allTagsMatch(additionalFilters, cdapTags5));
  }

  @Test
  public void testLabels() {
    CloudMonitoringWriter metricsWriter = new CloudMonitoringWriter();
    MonitoringConfig monitoringConfig = metricsWriter.getConfig(TEST_CONFIG_PATH);
    MetricsMapping metricsMapping = monitoringConfig.getMetricsMapping().get("program.completed.runs");

    //Random value for test
    String testValue = UUID.randomUUID().toString();
    Map<String, String> tags = ImmutableMap.of("app", testValue);
    Map<String, String> expected = ImmutableMap.of("pipeline_id", testValue);
    Map<String, String> actual = metricsWriter.getLabels(metricsMapping.getResourceLabels(), tags);
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testAutoFillingLabels() {
    CloudMonitoringWriter metricsWriter = new CloudMonitoringWriter();
    List<String> testLabels = ImmutableList.of("label1", "label2");
    Map<String, String> testLabelMap = ImmutableMap.of("label1", "value1", "label2", "value2", "label3", "value3");
    Map<String, String> expectedLabels = ImmutableMap.of("label1", "value1", "label2", "value2");
    Map<String, String> actualLabels = metricsWriter.getAutoFilledLabels(testLabels, testLabelMap);
    Assert.assertEquals(expectedLabels, actualLabels);
  }
}
