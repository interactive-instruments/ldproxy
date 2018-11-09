package de.ii.ldproxy.target.geojson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import de.ii.ldproxy.target.geojson.GeoJsonGeometryMapping.GEO_JSON_GEOMETRY_TYPE;
import de.ii.ldproxy.wfs3.api.FeatureTransformationContext;
import de.ii.xtraplatform.crs.api.CoordinatesWriterType;
import org.immutables.value.Value;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public abstract class FeatureTransformationContextGeoJson implements FeatureTransformationContext {

    @Override
    @Value.Default
    public ModifiableStateGeoJson getState() {
        return ModifiableStateGeoJson.create();
    }

    public abstract GeoJsonConfig getGeoJsonConfig();

    @Value.Default
    protected JsonGenerator getJsonGenerator() {
        JsonGenerator json = null;
        try {
            json = new JsonFactory().createGenerator(getOutputStream());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        json.setCodec(new ObjectMapper());
        //if (useFormattedJsonOutput) {
        json.useDefaultPrettyPrinter();
        //}
        if (getDebugJson()) {
            // Zum JSON debuggen hier einschalten.
            json = new JsonGeneratorDebug(json);
        }

        return json;
    }

    @Value.Default
    public boolean getDebugJson() {
        return false;
    }

    //TODO: to state
    private TokenBuffer tokenBuffer;
    protected TokenBuffer getJsonBuffer() {
        return tokenBuffer;
    }

    //@Value.Derived
    private TokenBuffer createJsonBuffer() {
        TokenBuffer json = new TokenBuffer(new ObjectMapper(), false);

        //if (useFormattedJsonOutput) {
        json.useDefaultPrettyPrinter();
        //}
        return json;
    }

    public JsonGenerator getJson() {
        return getState().isBuffering() ? getJsonBuffer() : getJsonGenerator();
    }

    public final void startBuffering() throws IOException {
        getJsonGenerator().flush();
        this.tokenBuffer = createJsonBuffer();
        getState().setIsBuffering(true);
    }

    public final void stopBuffering() throws IOException {
        if (getState().isBuffering()) {
            getState().setIsBuffering(false);
            //getJsonBuffer().serialize(getJsonGenerator());
            getJsonBuffer().close();
        }
    }

    public final void flushBuffer() throws IOException {
        getJsonBuffer().serialize(getJsonGenerator());
        getJsonBuffer().flush();
    }

    @Value.Modifiable
    public static abstract class StateGeoJson extends State {

        public abstract Optional<GEO_JSON_GEOMETRY_TYPE> getCurrentGeometryType();

        public abstract Optional<CoordinatesWriterType.Builder> getCoordinatesWriterBuilder();

        @Value.Default
        public int getCurrentGeometryNestingChange() {
            return 0;
        }

        @Value.Default
        public boolean isBuffering() {
            return false;
        }
    }
}
