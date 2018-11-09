package de.ii.ldproxy.target.geojson;

import com.fasterxml.jackson.core.JsonGenerator;
import de.ii.ldproxy.wfs3.api.FeatureWriterGeoJson;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static de.ii.ldproxy.wfs3.api.Wfs3ServiceData.DEFAULT_CRS;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class GeoJsonWriterCrs implements GeoJsonWriter {

    @Override
    public GeoJsonWriterCrs create() {
        return new GeoJsonWriterCrs();
    }

    @Override
    public int getSortPriority() {
        return 50;
    }

    @Override
    public void onStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (transformationContext.isFeatureCollection()) {
            writeCrs(transformationContext.getJson(), transformationContext.getCrsTransformer());
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    @Override
    public void onFeatureStart(FeatureTransformationContextGeoJson transformationContext, Consumer<FeatureTransformationContextGeoJson> next) throws IOException {
        if (!transformationContext.isFeatureCollection()) {
            writeCrs(transformationContext.getJson(), transformationContext.getCrsTransformer());
        }

        // next chain for extensions
        next.accept(transformationContext);
    }

    private void writeCrs(JsonGenerator json, Optional<CrsTransformer> crsTransformer) throws IOException {
        if (crsTransformer.isPresent() && !Objects.equals(crsTransformer.get().getTargetCrs(), DEFAULT_CRS)) {
            json.writeStringField("crs", crsTransformer.get().getTargetCrs().getAsUri());
        }
    }
}
