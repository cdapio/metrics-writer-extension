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

import com.google.api.Metric;
import com.google.api.MonitoredResource;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.common.collect.Lists;
import com.google.monitoring.v3.CreateTimeSeriesRequest;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.monitoring.v3.TypedValue;
import com.google.protobuf.util.Timestamps;
import io.cdap.cdap.api.metrics.MetricType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Util class for GoogleCloudMonitoring
 */
public final class CloudMonitoringUtil {

  // Max time series per request - Limit is 200 based on https://cloud.google.com/monitoring/quotas
  private static final int MAX_TIMESERIES_PER_REQUEST = 150;

  private CloudMonitoringUtil() {

  }

  /**
   * Convert CDAP {@link TimeSeriesMetadata} and their values to list of {@link TimeSeries} values.
   * Creates time series points, metric and monitored resource and builds the {@link TimeSeries} object that
   * can be send to the monitoring API.
   *
   * @param timeSeries       Map of {@link TimeSeriesMetadata} and their long values
   * @param startTimeSeconds start timestamp
   * @param endTimeSeconds   end timestamp
   * @return {@link List<TimeSeries>} values
   */
  public static List<TimeSeries> convertToMonitoringTimeSeries(Map<TimeSeriesMetadata, List<Long>> timeSeries,
                                                               long startTimeSeconds, long endTimeSeconds) {

    List<TimeSeries> timeSeriesList = new ArrayList<>();
    for (Map.Entry<TimeSeriesMetadata, List<Long>> entry : timeSeries.entrySet()) {
      TimeSeriesMetadata metadata = entry.getKey();
      long value = getAggregateValue(entry.getValue(), metadata.getAggregation());
      TimeInterval interval = TimeInterval.newBuilder()
        .setStartTime(Timestamps.fromSeconds(metadata.getMetricKind() == MetricType.GAUGE ?
                                               endTimeSeconds : startTimeSeconds))
        .setEndTime(Timestamps.fromSeconds(endTimeSeconds))
        .build();

      TypedValue typedValue = TypedValue.newBuilder().setInt64Value(value).build();
      Point point = Point.newBuilder().setInterval(interval).setValue(typedValue).build();
      // Prepares the metric descriptor
      Metric metric = Metric.newBuilder()
        .setType(metadata.getMetricType())
        .putAllLabels(metadata.getMetricLabels())
        .build();
      // Prepares the monitored resource descriptor
      MonitoredResource resource = MonitoredResource.newBuilder()
        .setType(metadata.getResourceType())
        .putAllLabels(metadata.getResourceLabels())
        .build();
      // Prepares the time series
      TimeSeries series = TimeSeries.newBuilder()
        .setMetric(metric)
        .setResource(resource)
        .addAllPoints(Collections.singletonList(point))
        .build();
      timeSeriesList.add(series);
    }
    return timeSeriesList;
  }

  /**
   * Send the {@link TimeSeries} list to cloud monitoring using {@link MetricServiceClient}
   *
   * @param timeSeriesList      {@link List<TimeSeries>} values
   * @param metricServiceClient {@link MetricServiceClient}
   * @param projectName         Project name string where the metrics will be sent.
   * @throws com.google.api.gax.rpc.ApiException if remote call fails.
   */
  public static void send(MetricServiceClient metricServiceClient, String projectName,
                          List<TimeSeries> timeSeriesList) {
    // Use fully qualified name for the project
    String fullProjectName = ProjectName.of(projectName).toString();
    for (List<TimeSeries> partitionedList : Lists.partition(timeSeriesList, MAX_TIMESERIES_PER_REQUEST)) {
      CreateTimeSeriesRequest request = CreateTimeSeriesRequest.newBuilder()
        .setName(fullProjectName)
        .addAllTimeSeries(partitionedList)
        .build();
      metricServiceClient.createTimeSeries(request);
    }
  }

  private static long getAggregateValue(List<Long> timeSeriesValues, AggregationType aggregation) {
    switch (aggregation) {
      case MEAN:
        OptionalDouble average = timeSeriesValues.stream().mapToLong(value -> value).average();
        return average.isPresent() ? (long) average.getAsDouble() : 0L;
      case SUM:
      default:
        //return SUM as default
        return timeSeriesValues.stream().reduce(0L, Long::sum);
    }
  }
}
