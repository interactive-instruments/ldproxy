/**
 * Copyright 2016 interactive instruments GmbH
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
package de.ii.ogc.wfs.proxy;

import com.google.common.collect.ImmutableList;
import de.ii.ldproxy.service.IndexMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zahnen
 */
public class WfsProxyFeatureTypeMapping {
    // TODO: multiplicity
    private Map<String, Map<String, List<TargetMapping>>> mappings;

    public WfsProxyFeatureTypeMapping() {
        this.mappings = new HashMap<>();
    }

    public void addMapping(String path, String targetType, TargetMapping targetMapping) {
        if (!mappings.containsKey(path)) {
            mappings.put(path, new HashMap<String, List<TargetMapping>>());
        }
        if (!mappings.get(path).containsKey(targetType)) {
            mappings.get(path).put(targetType, new ArrayList<TargetMapping>());
        }
        mappings.get(path).get(targetType).add(targetMapping);
    }

    public List<TargetMapping> findMappings(String path, String targetType) {
        if (mappings.containsKey(path) && mappings.get(path).containsKey(targetType)) {
               return mappings.get(path).get(targetType);
        }
        return ImmutableList.<TargetMapping>of();
    }

    public Map<String, List<TargetMapping>> findMappings(String targetType) {
        Map<String, List<TargetMapping>> mappings = new HashMap<>();

        for (String path: getMappings().keySet()) {
            if (getMappings().get(path).containsKey(targetType)) {
                mappings.put(path, getMappings().get(path).get(targetType));
            }
        }

        return mappings;
    }

    public Map<String, Map<String, List<TargetMapping>>> getMappings() {
        return mappings;
    }

    public void setMappings(Map<String, Map<String, List<TargetMapping>>> mappings) {
        this.mappings = mappings;
    }
}
