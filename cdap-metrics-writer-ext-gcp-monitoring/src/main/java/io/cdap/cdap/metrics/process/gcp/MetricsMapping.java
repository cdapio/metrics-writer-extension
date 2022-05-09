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

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Class that holds specifics for cloud monitoring metric with label key/value mappings and
 * tags to collect from the CDAP metric.
 * An example below.
 * {
 * "metricType": "datafusion.googleapis.com/odf_pipeline/runs_completed_count",
 * "resourceType": "datafusion.googleapis.com/ODFPipeline",
 * <p>
 * "resourceLabels": [
 * {"label": "pipeline_id", "value": "app", "valueIsTag": true}
 * ],
 * <p>
 * "autoFillLabels":  [
 * "resource_container", "org_id", "location", "cluster_id", "instance_id"
 * ],
 * <p>
 * "metricLabels": [
 * {"label": "complete_state", "value": "completed", "valueIsTag": false}
 * ],
 * <p>
 * "tagFilters": [
 * {"tag": "prg", "values": "DataPipelineWorkflow,DataStreamsSparkStreaming"}
 * ]
 * }
 */
public class MetricsMapping {

  private final String metricType;
  private final String resourceType;
  private final List<LabelMapping> resourceLabels;
  private final List<String> autoFillLabels;
  private final List<LabelMapping> metricLabels;
  private final List<TagFilter> tagFilters;
  private final AggregationType aggregation;

  public MetricsMapping(String metricType, String resourceType, List<LabelMapping> resourceLabels,
                        List<String> autoFillLabels, List<LabelMapping> metricLabels, List<TagFilter> tagFilters,
                        @Nullable AggregationType aggregation) {
    this.metricType = metricType;
    this.resourceType = resourceType;
    this.resourceLabels = resourceLabels;
    this.autoFillLabels = autoFillLabels;
    this.metricLabels = metricLabels;
    this.tagFilters = tagFilters;
    this.aggregation = aggregation;
  }

  public String getMetricType() {
    return metricType;
  }

  public String getResourceType() {
    return resourceType;
  }

  public List<LabelMapping> getResourceLabels() {
    return resourceLabels == null ? Collections.emptyList() : Collections.unmodifiableList(resourceLabels);
  }

  public List<String> getAutoFillLabels() {
    return autoFillLabels;
  }

  public List<LabelMapping> getMetricLabels() {
    return metricLabels == null ? Collections.emptyList() : Collections.unmodifiableList(metricLabels);
  }

  public List<TagFilter> getTagFilters() {
    return tagFilters == null ? Collections.emptyList() : Collections.unmodifiableList(tagFilters);
  }

  public AggregationType getAggregation() {
    return aggregation == null ? AggregationType.SUM : aggregation;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("MetricsMapping{metricType=").append(metricType);
    sb.append(", resourceType=").append(resourceType);
    sb.append(", resourceLabels=").append(resourceLabels);
    sb.append(", metricLabels=").append(metricLabels);
    sb.append(", tagFilters=").append(tagFilters);
    sb.append('}');
    return sb.toString();
  }
}
