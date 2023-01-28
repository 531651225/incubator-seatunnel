/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.connectors.doris.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public enum DorisSinkSemantics {
    NON("non"), EXACTLY_ONCE("exactly-once"), AT_LEAST_ONCE("at-least-once");

    private String name;

    public static DorisSinkSemantics fromName(String name) {
        List<DorisSinkSemantics> rs = Arrays.stream(DorisSinkSemantics.values()).filter(v -> v.getName().equals(name))
                .collect(Collectors.toList());
        return rs.isEmpty() ? null : rs.get(0);
    }
}