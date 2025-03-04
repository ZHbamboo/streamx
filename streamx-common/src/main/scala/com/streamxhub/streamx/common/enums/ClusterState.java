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

package com.streamxhub.streamx.common.enums;

import java.io.Serializable;

/**
 * @author: xxyykkxx
 * @since: 1.2.3
 */
public enum ClusterState implements Serializable {
    /**
     * 集群刚创建但未启动
     */
    CREATED(0),
    /**
     * 集群已启动
     */
    STARTED(1),
    /**
     * 集群已停止
     */
    STOPED(2);

    private final Integer value;

    ClusterState(Integer value) {
        this.value = value;
    }

    public static ClusterState of(Integer value) {
        for (ClusterState clusterState : values()) {
            if (clusterState.value.equals(value)) {
                return clusterState;
            }
        }
        return null;
    }

    public Integer getValue() {
        return value;
    }
}
