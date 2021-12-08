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

/**
 * Has the name/value mapping for Cloud monitoring label for a metric/resource and
 * details of value collection from a CDAP metric tag.
 * Some examples below.
 * {"label": "complete_state", "value": "completed", "valueIsTag": false}
 * {"label": "pipeline_id", "value": "app", "valueIsTag": true},
 * {"label": "compute_engine", "value": "N/A", "valueIsTag": false}
 */
public class LabelMapping {

  private final String label;
  private final String value;
  // If true, value is the name of a CDAP tag.
  private final boolean valueIsTag;

  public LabelMapping(String label, String value, boolean valueIsTag) {
    this.label = label;
    this.value = value;
    this.valueIsTag = valueIsTag;
  }

  public String getLabel() {
    return label;
  }

  public String getValue() {
    return value;
  }

  public boolean getValueIsTag() {
    return valueIsTag;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("LabelMapping{label=").append(label);
    sb.append(", value=").append(value);
    sb.append(", valueIsTag=").append(valueIsTag);
    sb.append('}');
    return sb.toString();
  }
}
