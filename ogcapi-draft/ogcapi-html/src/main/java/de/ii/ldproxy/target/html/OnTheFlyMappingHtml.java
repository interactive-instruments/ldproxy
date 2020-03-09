/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.target.html;

import de.ii.xtraplatform.features.domain.legacy.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.OnTheFlyMapping;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml.GML_GEOMETRY_TYPE;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
class OnTheFlyMappingHtml implements OnTheFlyMapping {
    @Override
    public TargetMapping getTargetMappingForFeatureType(String path) {
        MicrodataPropertyMapping microdataPropertyMapping = new MicrodataPropertyMapping();
        microdataPropertyMapping.setName(path);
        microdataPropertyMapping.setItemType("http://schema.org/Place");
        microdataPropertyMapping.setEnabled(true);

        return microdataPropertyMapping;
    }

    @Override
    public TargetMapping getTargetMappingForAttribute(List<String> path, String value) {
        //TODO: only if path = gml:id
        if (path.size() == 1 && (path.get(0)
                                     .endsWith("@id") || path.get(0)
                                                             .endsWith("@fid"))) {
            MicrodataPropertyMapping microdataPropertyMapping = new MicrodataPropertyMapping();
            microdataPropertyMapping.setName("id");
            microdataPropertyMapping.setType(MicrodataMapping.MICRODATA_TYPE.ID);
            microdataPropertyMapping.setEnabled(true);
            microdataPropertyMapping.setShowInCollection(true);

            return microdataPropertyMapping;
        }
        return null;
    }

    @Override
    public TargetMapping getTargetMappingForProperty(List<String> path, String value) {
        MicrodataPropertyMapping microdataPropertyMapping = new MicrodataPropertyMapping();
        microdataPropertyMapping.setName(path.stream()
                                             .map(s -> s.contains(":") ? s.substring(s.lastIndexOf(":") + 1) : s)
                                             .collect(Collectors.joining(".")));
        microdataPropertyMapping.setType(MicrodataMapping.MICRODATA_TYPE.STRING);
        microdataPropertyMapping.setEnabled(true);
        microdataPropertyMapping.setShowInCollection(true);

        return microdataPropertyMapping;
    }

    @Override
    public TargetMapping getTargetMappingForGeometry(List<String> path) {

        String localName = path.size() > 0 ? (path.get(path.size() - 1)
                                                  .contains(":") ? path.get(path.size() - 1)
                                                                       .substring(path.get(path.size() - 1)
                                                                                      .lastIndexOf(":") + 1) : path.get(path.size() - 1)) : "";
        //TODO
        if (GML_GEOMETRY_TYPE.fromString(localName).isValid()) {

            MicrodataGeometryMapping microdataGeometryMapping = new MicrodataGeometryMapping();
            microdataGeometryMapping.setEnabled(true);
            microdataGeometryMapping.setShowInCollection(true);
            microdataGeometryMapping.setType(MicrodataMapping.MICRODATA_TYPE.GEOMETRY);
            microdataGeometryMapping.setGeometryType(MicrodataGeometryMapping.MICRODATA_GEOMETRY_TYPE.forGmlType(GML_GEOMETRY_TYPE.fromString(localName)
                                                                                                                                  .toSimpleFeatureGeometry()));

            return microdataGeometryMapping;
        }
        return null;
    }
}
