/**
 * Copyright 2017 European Union, interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.geojson;

import de.ii.ldproxy.target.geojson.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.xtraplatform.features.domain.legacy.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.OnTheFlyMapping;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml.GML_GEOMETRY_TYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class GeoJsonOnTheFlyMapping implements OnTheFlyMapping {
    List<String> paths;

    GeoJsonOnTheFlyMapping() {
        this.paths = new ArrayList<>();
    }


    @Override
    public TargetMapping getTargetMappingForFeatureType(String path) {
        return null;
    }

    @Override
    public TargetMapping getTargetMappingForAttribute(List<String> path, String value) {
        if (path.size() == 1 && (path.get(0)
                                     .endsWith("@id") || path.get(0)
                                                             .endsWith("@fid"))) {
            GeoJsonPropertyMapping targetMapping = new GeoJsonPropertyMapping();
            targetMapping.setEnabled(true);
            targetMapping.setName("id");
            targetMapping.setType(GeoJsonMapping.GEO_JSON_TYPE.ID);

            return targetMapping;
        }

        return null;
    }

    @Override
    public TargetMapping getTargetMappingForProperty(List<String> path, String value) {

        // TODO: parse value to detect type
        GeoJsonMapping.GEO_JSON_TYPE dataType = GeoJsonMapping.GEO_JSON_TYPE.STRING;
        String field = getFieldName(path);

        if (!value.isEmpty() && dataType.isValid() && !hasPath(field)) {

            GeoJsonPropertyMapping targetMapping = new GeoJsonPropertyMapping();
            targetMapping.setEnabled(true);
            targetMapping.setName(field);
            targetMapping.setType(dataType);


            return targetMapping;
        }

        return null;
    }

    @Override
    public TargetMapping getTargetMappingForGeometry(List<String> path) {
        String localName = getLocalName(path);

        GEO_JSON_GEOMETRY_TYPE geoType = GEO_JSON_GEOMETRY_TYPE.forGmlType(GML_GEOMETRY_TYPE.fromString(localName)
                                                                                            .toSimpleFeatureGeometry());

        if (geoType.isValid()) {

            GeoJsonGeometryMapping targetMapping = new GeoJsonGeometryMapping();
            targetMapping.setEnabled(true);
            targetMapping.setType(GeoJsonMapping.GEO_JSON_TYPE.GEOMETRY);
            targetMapping.setGeometryType(geoType);

            String field = getFieldName(path);
            paths.add(field);

            return targetMapping;
        }

        return null;
    }

    private String getLocalName(List<String> path) {
        return path.size() > 0 ? (path.get(path.size() - 1)
                                      .contains(":") ? path.get(path.size() - 1)
                                                           .substring(path.get(path.size() - 1)
                                                                          .lastIndexOf(":") + 1) : path.get(path.size() - 1)) : "";
    }

    private String getFieldName(List<String> path) {
        return path.stream()
                   .map(s -> s.contains(":") ? s.substring(s.lastIndexOf(":") + 1) : s)
                   .collect(Collectors.joining("."));
    }

    private boolean hasPath(String path) {
        for (String p : paths) {
            if (path.startsWith(p)) {
                return true;
            }
        }
        return false;
    }
}
