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

import java.util.Arrays;
import java.util.List;

/**
 * A tag filter with tag name and a comma separated list of permissible values for the cloud monitoring metric label.
 * Example below.
 * {"tag": "prg", "values": "DataPipelineWorkflow,DataStreamsSparkStreaming"}
 */
public class TagFilter {

  private final String tag;
  private final String values;

  public TagFilter(String tag, String values) {
    this.tag = tag;
    this.values = values;
  }

  public String getTag() {
    return tag;
  }

  public List<String> getValuesList() {
    return Arrays.asList(values.split(","));
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("TagFilter{tag=").append(tag);
    sb.append(", values=").append(values);
    sb.append('}');
    return sb.toString();
  }
}
