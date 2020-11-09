package de.ii.ldproxy.ogcapi.features.core.app;

import de.ii.ldproxy.ogcapi.domain.ExtensionConfiguration;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.HttpMethods;
import de.ii.ldproxy.ogcapi.domain.OgcApiQueryParameter;
import de.ii.ldproxy.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component
@Provides
@Instantiate
public class QueryParameterBbox implements OgcApiQueryParameter {

    private final Schema baseSchema;

    public QueryParameterBbox() {
        // TODO support 6 coordinates (note: maxItems was originally set to 4 for now, but the CITE tests require maxItems=6)
        baseSchema = new ArraySchema().items(new NumberSchema().format("double")).minItems(4).maxItems(6);
    }

    @Override
    public String getName() {
        return "bbox";
    }

    @Override
    public String getDescription() {
        return "Only features that have a geometry that intersects the bounding box are selected. " +
                "The bounding box is provided as four or six numbers, depending on whether the coordinate reference system " +
                "includes a vertical axis (height or depth):\n\n" +
                "* Lower left corner, coordinate axis 1 \n" +
                "* Lower left corner, coordinate axis 2 \n" +
                "* Minimum value, coordinate axis 3 (optional) \n" +
                "* Upper right corner, coordinate axis 1 \n" +
                "* Upper right corner, coordinate axis 2 \n" +
                "* Maximum value, coordinate axis 3 (optional) \n\n" +
                "The coordinate reference system of the values is WGS 84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84) " +
                "unless a different coordinate reference system is specified in the parameter `bbox-crs`. " +
                "For WGS 84 longitude/latitude the values are in most cases the sequence of minimum longitude, " +
                "minimum latitude, maximum longitude and maximum latitude. " +
                "However, in cases where the box spans the antimeridian the first value (west-most box edge) is larger " +
                "than the third value (east-most box edge). If the vertical axis is included, the third and " +
                "the sixth number are the bottom and the top of the 3-dimensional bounding box. \n" +
                "If a feature has multiple spatial geometry properties, it is the decision of the server " +
                "whether only a single spatial geometry property is used to determine the extent or all relevant geometries.";
    }

    @Override
    public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
        return isEnabledForApi(apiData) &&
               method== HttpMethods.GET &&
               definitionPath.equals("/collections/{collectionId}/items");
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData) {
        return baseSchema;
    }

    @Override
    public Schema getSchema(OgcApiDataV2 apiData, String collectionId) {
        return baseSchema;
    }

    @Override
    public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
        return FeaturesCoreConfiguration.class;
    }
}
