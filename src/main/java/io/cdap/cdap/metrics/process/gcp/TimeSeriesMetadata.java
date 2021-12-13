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

import io.cdap.cdap.api.metrics.MetricType;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Stores cloud monitoring metric details for a CDAP metric.
 */
public class TimeSeriesMetadata {

  private final String metricType;
  private final String resourceType;
  private final Map<String, String> metricLabels;
  private final Map<String, String> resourceLabels;
  private final MetricType metricKind;
  private final AggregationType aggregation;

  public TimeSeriesMetadata(String metricType, String resourceType,
                            Map<String, String> metricLabels, Map<String, String> resourceLabels,
                            MetricType metricKind, @Nullable AggregationType aggregation) {
    this.metricType = metricType;
    this.resourceType = resourceType;
    this.metricLabels = Collections.unmodifiableMap(metricLabels);
    this.resourceLabels = Collections.unmodifiableMap(resourceLabels);
    this.metricKind = metricKind;
    this.aggregation = aggregation;
  }

  public String getMetricType() {
    return metricType;
  }

  public String getResourceType() {
    return resourceType;
  }

  public Map<String, String> getMetricLabels() {
    return metricLabels;
  }

  public Map<String, String> getResourceLabels() {
    return resourceLabels;
  }

  public MetricType getMetricKind() {
    return metricKind;
  }

  public AggregationType getAggregation() {
    return aggregation == null ? AggregationType.SUM : aggregation;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    TimeSeriesMetadata other = (TimeSeriesMetadata) obj;
    return Objects.equals(metricType, other.metricType) &&
      Objects.equals(resourceType, other.resourceType) &&
      Objects.equals(metricLabels, other.metricLabels) &&
      Objects.equals(resourceLabels, other.resourceLabels);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("TimeSeriesMetadata{metricType=").append(metricType);
    sb.append(", metricKind=").append(metricKind);
    sb.append(", resourceType=").append(resourceType);
    sb.append(", metricLabels=").append(metricLabels);
    sb.append(", resourceLabels=").append(resourceLabels);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(metricType, resourceType, metricLabels, resourceLabels);
  }
}
