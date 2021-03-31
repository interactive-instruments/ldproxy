package de.ii.ldproxy.ogcapi.features.core.domain;

import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;

import java.util.List;
import java.util.Map;

public interface SchemaInfo {

    List<String> getPropertyNames(FeatureSchema featureType, boolean withArrayBrackets);

    Map<String, String> getNameTitleMap(FeatureSchema featureType);

    List<String> getPropertyNames(OgcApiDataV2 apiData, String collectionId);

    List<String> getPropertyNames(OgcApiDataV2 apiData, String collectionId, boolean withSpatial, boolean withArrayBrackets);

    Map<String, SchemaBase.Type> getPropertyTypes(FeatureSchema featureType, boolean withArrayBrackets);
}
