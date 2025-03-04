/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.kubernetes.model

import com.streamxhub.streamx.flink.kubernetes.enums.FlinkK8sExecuteMode

/**
 * flink cluster identifier on kubernetes
 * author:Al-assad
 */
case class ClusterKey(executeMode: FlinkK8sExecuteMode.Value,
                      namespace: String = "default",
                      clusterId: String)

object ClusterKey {
  def of(trackId: TrackId): ClusterKey = ClusterKey(trackId.executeMode, trackId.namespace, trackId.clusterId)
}

