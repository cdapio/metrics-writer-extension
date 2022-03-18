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
import java.util.Map;

/**
 * Metrics writer configuration that holds the mapping between CDAP metrics and Cloud monitoring metrics
 * Example below.
 * {
 *   "mapping": {
 *     "program.completed.runs": {
 *       "metricType": "datafusion.googleapis.com/odf_pipeline/runs_completed_count",
 *       "resourceType": "datafusion.googleapis.com/ODFPipeline",
 *       "resourceLabels": [
 *         {"label": "pipeline_id", "value": "app", "valueIsTag": true}
 *       ],
 *       "autoFillLabels":  [
 *         "resource_container", "org_id", "location", "anthos_cluster", "instance_id"
 *       ],
 *       "metricLabels": [
 *         {"label": "complete_state", "value": "completed", "valueIsTag": false}
 *       ],
 *       "tagFilters": [
 *         {"tag": "prg", "values": "DataPipelineWorkflow,DataStreamsSparkStreaming"}
 *       ]
 *       }
 *     }
 * }
 */
public class MonitoringConfig {

  public static final MonitoringConfig EMPTY = new MonitoringConfig(Collections.emptyMap());

  private final Map<String, MetricsMapping> mapping;

  public MonitoringConfig(Map<String, MetricsMapping> mapping) {
    this.mapping = mapping;
  }

  public Map<String, MetricsMapping> getMetricsMapping() {
    // can be null when deserialized through gson
    return mapping == null ? Collections.emptyMap() : Collections.unmodifiableMap(mapping);
  }
}
