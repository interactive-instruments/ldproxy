package de.ii.ldproxy.wfs3.crs;

import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.OgcApiApiDataV2;
import de.ii.xtraplatform.crs.domain.EpsgCrs;

import java.util.List;

public interface CrsSupport {

    List<EpsgCrs> getSupportedCrsList(OgcApiApiDataV2 apiData);

    List<EpsgCrs> getSupportedCrsList(OgcApiApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureTypeConfiguration);

    boolean isSupported(OgcApiApiDataV2 apiData, EpsgCrs crs);

    boolean isSupported(OgcApiApiDataV2 apiData, FeatureTypeConfigurationOgcApi featureTypeConfiguration, EpsgCrs crs);
}
