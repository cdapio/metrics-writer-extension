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

import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.cloud.monitoring.v3.MetricServiceSettings;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.monitoring.v3.TimeSeries;
import io.cdap.cdap.api.metrics.MetricType;
import io.cdap.cdap.api.metrics.MetricValue;
import io.cdap.cdap.api.metrics.MetricValues;
import io.cdap.cdap.api.metrics.MetricsWriter;
import io.cdap.cdap.api.metrics.MetricsWriterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * {@link MetricsWriter} implementation for writing metrics to Google Cloud Monitoring
 */
public class CloudMonitoringWriter implements MetricsWriter {

  private static final Logger LOG = LoggerFactory.getLogger(CloudMonitoringWriter.class);
  private static final Gson GSON = new Gson();
  private static final String RESOURCE_CONTAINER = "resource_container";
  private static final String PROJECT = "project";
  private static final String ORG_ID = "org_id";
  private static final String LOCATION = "location";
  private static final String ANTHOS_CLUSTER = "anthos_cluster";
  private static final String INSTANCE_ID = "instance_id";
  private static final String VERSION = "version";
  private static final String WRITE_FREQUENCY_SECONDS = "write.frequency.seconds";
  private static final String MONITORING_ENDPOINT = "monitoring.endpoint";
  private static final String CONFIG_FILE_PATH = "config.file.path";
  private static final String WRITER_NAME = "google_cloud_monitoring_writer";

  private final AtomicLong lastEndTime = new AtomicLong(-1);
  private Map<String, MetricsMapping> metricsMapping;
  @Nullable
  private MetricServiceClient metricServiceClient;
  @Nullable
  private String projectName;
  private Map<String, String> autoFilledLabelMap;
  private int pollFreqInSeconds;

  public CloudMonitoringWriter() {

  }

  @Override
  public void write(Collection<MetricValues> metricValues) {
    if (!isInitComplete()) {
      return;
    }

    Map<TimeSeriesMetadata, List<Long>> timeSeriesMap = createTimeSeriesMap(metricValues);
    long endTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    long startTimeSeconds = (lastEndTime.get() == -1 || lastEndTime.get() == endTimeSeconds) ?
      endTimeSeconds - pollFreqInSeconds : lastEndTime.get();
    List<TimeSeries> timeSeries = CloudMonitoringUtil.convertToMonitoringTimeSeries(timeSeriesMap,
                                                                                    startTimeSeconds,
                                                                                    endTimeSeconds);
    CloudMonitoringUtil.send(metricServiceClient, projectName, timeSeries);
    lastEndTime.getAndUpdate(value -> Math.max(value, endTimeSeconds));
  }

  private boolean isInitComplete() {
    if (metricServiceClient == null) {
      LOG.debug(
        "MetricServiceClient is not available. Returning with out writing values in GoogleCloudMonitoringWriter.");
      return false;
    }
    if (projectName == null) {
      LOG.debug(
        "Project name is not configured. Returning with out writing values in GoogleCloudMonitoringWriter.");
      return false;
    }
    if (metricsMapping.isEmpty()) {
      LOG.debug(
        "Metrics are not mapped. Returning with out writing values in GoogleCloudMonitoringWriter.");
      return false;
    }
    return true;
  }

  @Override
  public void initialize(MetricsWriterContext metricsWriterContext) {
    if (this.metricServiceClient != null) {
      LOG.debug("GoogleCloudMonitoringWriter is already initialized");
      return;
    }

    MonitoringConfig config = getConfig(metricsWriterContext.getProperties().get(CONFIG_FILE_PATH));
    this.metricsMapping = config.getMetricsMapping();
    this.projectName = metricsWriterContext.getProperties().get(PROJECT);
    this.pollFreqInSeconds = Integer.parseInt(metricsWriterContext.getProperties().get(WRITE_FREQUENCY_SECONDS));
    populateAutoFilledMap(metricsWriterContext.getProperties(), metricsWriterContext.getPlatformVersion());

    try {
      String endPoint = metricsWriterContext.getProperties().get(MONITORING_ENDPOINT);
      MetricServiceSettings metricServiceSettings =
        MetricServiceSettings.newBuilder().setEndpoint(endPoint)
          .build();
      metricServiceClient = MetricServiceClient.create(metricServiceSettings);
    } catch (Exception ex) {
      LOG.error(
        "Exception while creating MetricServiceClient, Metrics will not be sent to Google Cloud Monitoring. ",
        ex);
    }
  }

  private void populateAutoFilledMap(Map<String, String> properties, String platformVersion) {
    autoFilledLabelMap = new HashMap<>();
    autoFilledLabelMap.put(RESOURCE_CONTAINER, properties.get(PROJECT));
    autoFilledLabelMap.put(ORG_ID, properties.get(ORG_ID));
    autoFilledLabelMap.put(LOCATION, properties.get(LOCATION));
    autoFilledLabelMap.put(ANTHOS_CLUSTER, properties.get(ANTHOS_CLUSTER));
    autoFilledLabelMap.put(INSTANCE_ID, properties.get(INSTANCE_ID));
    autoFilledLabelMap.put(VERSION, platformVersion);
    LOG.debug("Populated autoFilledLabelMap map {}", autoFilledLabelMap);
  }

  @Override
  public String getID() {
    return WRITER_NAME;
  }

  @Override
  public void close() {
    if (metricServiceClient == null) {
      return;
    }
    metricServiceClient.close();
  }

  MonitoringConfig getConfig(String configFilePath) {
    try (Reader reader = new FileReader(configFilePath)) {
      return GSON.fromJson(reader, MonitoringConfig.class);
    } catch (Exception ex) {
      LOG.info(
        "Exception while loading config from mapping file {}. Metrics will not be sent to Google Cloud Monitoring.",
        configFilePath, ex);
      return MonitoringConfig.EMPTY;
    }
  }

  private Map<TimeSeriesMetadata, List<Long>> createTimeSeriesMap(Collection<MetricValues> metricValues) {
    Map<TimeSeriesMetadata, List<Long>> timeSeries = new HashMap<>();
    for (MetricValues values : metricValues) {
      for (MetricValue metricValue : values.getMetrics()) {
        Optional<Map.Entry<String, MetricsMapping>> mappingEntry = getMappingEntry(metricsMapping, metricValue,
                                                                                   values.getTags());
        if (!mappingEntry.isPresent()) {
          continue;
        }
        MetricsMapping mapping = mappingEntry.get().getValue();
        TimeSeriesMetadata metadata = createTimeSeriesMetadata(mapping, metricValue, values.getTags());
        timeSeries.computeIfAbsent(metadata, timeSeriesMetadata -> new ArrayList<>()).add(metricValue.getValue());
      }
    }
    return timeSeries;
  }

  @VisibleForTesting
  Optional<Map.Entry<String, MetricsMapping>> getMappingEntry(Map<String, MetricsMapping> metricsMapping,
                                                              MetricValue metricValue, Map<String, String> tags) {
    return metricsMapping.entrySet()
      .stream()
      .filter(e -> metricValue.getName().equals(e.getKey()))
      .filter(e -> allTagsMatch(e.getValue().getTagFilters(), tags))
      .findFirst();
  }

  @VisibleForTesting
  boolean allTagsMatch(List<TagFilter> configTagFilters, Map<String, String> cdapTags) {
    return configTagFilters
      .stream()
      .allMatch(configTag ->
                  cdapTags.containsKey(configTag.getTag()) &&
                    configTag.getValuesList().contains(cdapTags.get(configTag.getTag())));
  }

  private TimeSeriesMetadata createTimeSeriesMetadata(MetricsMapping mapping, MetricValue metricValue,
                                                      Map<String, String> tags) {
    MetricType metricKind = metricValue.getType();
    Map<String, String> metricLabels = getLabels(mapping.getMetricLabels(), tags);
    Map<String, String> resourceLabels = getLabels(mapping.getResourceLabels(), tags);
    resourceLabels.putAll(getAutoFilledLabels(mapping.getAutoFillLabels(), this.autoFilledLabelMap));
    return new TimeSeriesMetadata(mapping.getMetricType(),
                                  mapping.getResourceType(),
                                  metricLabels, resourceLabels, metricKind,
                                  mapping.getAggregation());
  }

  @VisibleForTesting
  Map<String, String> getLabels(List<LabelMapping> labelMappings, Map<String, String> tags) {
    //If value is a tag, get from tags map
    Map<String, String> labels = labelMappings.stream().filter(LabelMapping::getValueIsTag)
      .filter(lm -> tags.get(lm.getValue()) != null)
      .collect(Collectors.toMap(LabelMapping::getLabel, lm -> tags.get(lm.getValue())));
    labels.putAll(labelMappings.stream().filter(lm -> !lm.getValueIsTag())
                    .collect(Collectors.toMap(LabelMapping::getLabel, LabelMapping::getValue)));
    return labels;
  }

  @VisibleForTesting
  Map<String, String> getAutoFilledLabels(List<String> autoFillRequests, Map<String, String> autoFilledLabelMap) {
    return autoFillRequests.stream()
      .collect(Collectors.toMap(request -> request, autoFilledLabelMap::get));
  }
}
