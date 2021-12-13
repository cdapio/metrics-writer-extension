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

import com.google.api.gax.rpc.UnaryCallable;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.TimeSeries;
import com.google.protobuf.Empty;
import io.cdap.cdap.api.metrics.MetricType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudMonitoringUtil.class, MetricServiceClient.class})
public class CloudMonitoringUtilTest {

  private static final String METRIC_TYPE = "datafusion.googleapis.com/odf_pipeline/runs_completed_count";
  private static final String RESOURCE_TYPE = "datafusion.googleapis.com/ODFPipeline";

  @Test
  public void testSend() {
    long endTime = System.currentTimeMillis() / 1000;
    long startTime = endTime - 100;
    Map<TimeSeriesMetadata, List<Long>> timeSeriesMap = new HashMap<>();
    TimeSeriesMetadata testTimeSeriesMetaData = getTestTimeSeriesMetaData(MetricType.COUNTER, AggregationType.MEAN);
    timeSeriesMap.put(testTimeSeriesMetaData, Arrays.asList(10L, 20L, 40L));
    List<TimeSeries> timeSeriesList = CloudMonitoringUtil.convertToMonitoringTimeSeries(timeSeriesMap, startTime,
                                                                                        endTime);
    MetricServiceClient mockServiceClient = PowerMockito.mock(MetricServiceClient.class);
    UnaryCallable<CreateTimeSeriesRequest, Empty> mockCallable =
      (UnaryCallable<CreateTimeSeriesRequest, Empty>) PowerMockito.mock(UnaryCallable.class);
    PowerMockito.when(mockServiceClient.createTimeSeriesCallable()).thenReturn(mockCallable);
    CloudMonitoringUtil.send(mockServiceClient, "test-project", timeSeriesList);
    ArgumentCaptor<CreateTimeSeriesRequest> argumentCaptor = ArgumentCaptor.forClass(CreateTimeSeriesRequest.class);
    Mockito.verify(mockServiceClient).createTimeSeries(argumentCaptor.capture());
    CreateTimeSeriesRequest timeSeriesRequest = argumentCaptor.getValue();
    Assert.assertEquals(1, timeSeriesRequest.getTimeSeriesCount());
    Assert.assertEquals("projects/test-project", timeSeriesRequest.getName());
    TimeSeries timeSeries = timeSeriesRequest.getTimeSeries(0);
    Assert.assertEquals(RESOURCE_TYPE, timeSeries.getResource().getType());
    Assert.assertEquals(METRIC_TYPE, timeSeries.getMetric().getType());
    Assert.assertEquals(1, timeSeries.getPointsCount());
    Assert.assertEquals(23L, timeSeries.getPoints(0).getValue().getInt64Value());
  }

  @Test
  public void testConvertToMonitoringTimeSeriesCounter() {
    long endTime = System.currentTimeMillis() / 1000;
    long startTime = endTime - 100;
    Map<TimeSeriesMetadata, List<Long>> timeSeriesMap = new HashMap<>();
    TimeSeriesMetadata testTimeSeriesMetaData = getTestTimeSeriesMetaData(MetricType.COUNTER, AggregationType.SUM);
    timeSeriesMap.put(testTimeSeriesMetaData, Arrays.asList(10L, 20L, 30L));
    List<TimeSeries> timeSeriesList = CloudMonitoringUtil.convertToMonitoringTimeSeries(timeSeriesMap, startTime,
                                                                                        endTime);
    Assert.assertEquals(1, timeSeriesList.size());
    TimeSeries timeSeries = timeSeriesList.get(0);
    Assert.assertEquals(testTimeSeriesMetaData.getMetricType(), timeSeries.getMetric().getType());
    Assert.assertEquals(testTimeSeriesMetaData.getResourceType(), timeSeries.getResource().getType());
    Assert.assertEquals(testTimeSeriesMetaData.getMetricLabels(), timeSeries.getMetric().getLabelsMap());
    Assert.assertEquals(testTimeSeriesMetaData.getResourceLabels(), timeSeries.getResource().getLabelsMap());
    Assert.assertEquals(1, timeSeries.getPointsCount());
    Assert.assertEquals(60, timeSeries.getPoints(0).getValue().getInt64Value());
    Assert.assertEquals(startTime, timeSeries.getPoints(0).getInterval().getStartTime().getSeconds());
    Assert.assertEquals(endTime, timeSeries.getPoints(0).getInterval().getEndTime().getSeconds());
  }

  @Test
  public void testConvertToMonitoringTimeSeriesGuage() {
    long endTime = System.currentTimeMillis() / 1000;
    long startTime = endTime - 100;
    Map<TimeSeriesMetadata, List<Long>> timeSeriesMap = new HashMap<>();
    TimeSeriesMetadata testTimeSeriesMetaData = getTestTimeSeriesMetaData(MetricType.GAUGE, AggregationType.SUM);
    timeSeriesMap.put(testTimeSeriesMetaData, Arrays.asList(10L, 20L, 30L));
    List<TimeSeries> timeSeriesList = CloudMonitoringUtil.convertToMonitoringTimeSeries(timeSeriesMap, startTime,
                                                                                        endTime);
    Assert.assertEquals(1, timeSeriesList.size());
    TimeSeries timeSeries = timeSeriesList.get(0);
    Assert.assertEquals(testTimeSeriesMetaData.getMetricType(), timeSeries.getMetric().getType());
    Assert.assertEquals(testTimeSeriesMetaData.getResourceType(), timeSeries.getResource().getType());
    Assert.assertEquals(testTimeSeriesMetaData.getMetricLabels(), timeSeries.getMetric().getLabelsMap());
    Assert.assertEquals(testTimeSeriesMetaData.getResourceLabels(), timeSeries.getResource().getLabelsMap());
    Assert.assertEquals(1, timeSeries.getPointsCount());
    Assert.assertEquals(60, timeSeries.getPoints(0).getValue().getInt64Value());
    Assert.assertEquals(endTime, timeSeries.getPoints(0).getInterval().getStartTime().getSeconds());
    Assert.assertEquals(endTime, timeSeries.getPoints(0).getInterval().getEndTime().getSeconds());
  }

  private TimeSeriesMetadata getTestTimeSeriesMetaData(MetricType type, AggregationType aggregationType) {
    Map<String, String> metricLabels = new HashMap<>();
    metricLabels.put("complete_state", "completed");

    Map<String, String> resourceLabels = new HashMap<>();
    resourceLabels.put("compute_engine", "N/A");
    resourceLabels.put("pipeline_id", "test-pipeline-1");

    return new TimeSeriesMetadata(METRIC_TYPE, RESOURCE_TYPE, metricLabels, resourceLabels, type, aggregationType);
  }
}
