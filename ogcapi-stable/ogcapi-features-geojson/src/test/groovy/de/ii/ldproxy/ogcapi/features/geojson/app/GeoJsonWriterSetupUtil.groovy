package de.ii.ldproxy.ogcapi.features.geojson.app

import com.google.common.collect.ImmutableList
import de.ii.ldproxy.ogcapi.domain.ApiMediaType
import de.ii.ldproxy.ogcapi.domain.ApiRequestContext
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDataV2
import de.ii.ldproxy.ogcapi.domain.OgcApi
import de.ii.ldproxy.ogcapi.domain.URICustomizer
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformationContextGeoJson
import de.ii.ldproxy.ogcapi.features.geojson.domain.FeatureTransformerGeoJson
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableFeatureTransformationContextGeoJson
import de.ii.ldproxy.ogcapi.features.geojson.domain.ImmutableGeoJsonConfiguration
import de.ii.ldproxy.ogcapi.features.geojson.domain.ModifiableStateGeoJson
import de.ii.xtraplatform.crs.domain.OgcCrs

import java.nio.charset.StandardCharsets

class GeoJsonWriterSetupUtil {

    static String asString(ByteArrayOutputStream outputStream) {
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8)
    }

    static FeatureTransformationContextGeoJson createTransformationContext(OutputStream outputStream, boolean isCollection) throws URISyntaxException {
        return ImmutableFeatureTransformationContextGeoJson.builder()
                .defaultCrs(OgcCrs.CRS84)
                .apiData(new ImmutableOgcApiDataV2.Builder()
                        .id("s")
                        .serviceType("OGC_API")
                /*.featureProvider(new ImmutableFeatureProviderDataTransformer.Builder()
                        .providerType("WFS")
                        .connectorType("HTTP")
                                                                .connectionInfo(new ImmutableConnectionInfoWfsHttp.Builder()
                                                                                                              .uri(new URI("http://localhost"))
                                                                                                              .method(ConnectionInfoWfsHttp.METHOD.GET)
                                                                                                              .version("2.0.0")
                                                                                                              .gmlVersion("3.2.1")
                                                                                                              .build())
                                                                .nativeCrs(new EpsgCrs())
                                                                .build())*/
                        .build())
                .collectionId("xyz")
                .outputStream(outputStream)
                .links(ImmutableList.of())
                .isFeatureCollection(isCollection)
                .ogcApiRequest(new ApiRequestContext() {
                    @Override
                    ApiMediaType getMediaType() {
                        return null
                    }

                    @Override
                    List<ApiMediaType> getAlternateMediaTypes() {
                        return null
                    }

                    @Override
                    Optional<Locale> getLanguage() {
                        return Optional.empty()
                    }

                    @Override
                    OgcApi getApi() {
                        return null
                    }

                    @Override
                    URICustomizer getUriCustomizer() {
                        return new URICustomizer()
                    }

                    @Override
                    String getStaticUrlPrefix() {
                        return null
                    }

                    @Override
                    Map<String, String> getParameters() {
                        return null
                    }
                })
                .limit(10)
                .offset(20)
                .maxAllowableOffset(0)
                .isHitsOnly(false)
                .state(ModifiableStateGeoJson.create())
                .geoJsonConfig(new ImmutableGeoJsonConfiguration.Builder().enabled(true).nestedObjectStrategy(FeatureTransformerGeoJson.NESTED_OBJECTS.NEST).multiplicityStrategy(FeatureTransformerGeoJson.MULTIPLICITY.ARRAY).useFormattedJsonOutput(true).build())
                .build()

    }

}
