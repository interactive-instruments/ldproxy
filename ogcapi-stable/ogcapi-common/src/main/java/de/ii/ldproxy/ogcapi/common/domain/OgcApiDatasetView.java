package de.ii.ldproxy.ogcapi.common.domain;

import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableMetadata;
import de.ii.ldproxy.ogcapi.domain.ImmutableTemporalExtent;
import de.ii.ldproxy.ogcapi.domain.Link;
import de.ii.ldproxy.ogcapi.domain.Metadata;
import de.ii.ldproxy.ogcapi.domain.OgcApiDataV2;
import de.ii.ldproxy.ogcapi.domain.TemporalExtent;
import de.ii.ldproxy.ogcapi.domain.URICustomizer;
import de.ii.ldproxy.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.NavigationDTO;
import de.ii.ldproxy.ogcapi.html.domain.OgcApiView;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;

import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.crs.domain.OgcCrs.CRS84;
import static de.ii.xtraplatform.dropwizard.domain.LambdaWithException.mayThrow;

public abstract class OgcApiDatasetView extends OgcApiView {

    protected final Optional<BoundingBox> bbox;
    protected final Optional<TemporalExtent> temporalExtent;
    protected final Optional<OgcApiExtentTemporal> temporalExtentIso;
    protected final URICustomizer uriCustomizer;

    protected OgcApiDatasetView(String templateName, @Nullable Charset charset, @Nullable OgcApiDataV2 apiData, List<NavigationDTO> breadCrumbs, HtmlConfiguration htmlConfig, boolean noIndex, String urlPrefix, @Nullable List<Link> links, @Nullable String title, @Nullable String description, URICustomizer uriCustomizer, Optional<OgcApiExtent> extent) {
        super(templateName, charset, apiData, breadCrumbs, htmlConfig, noIndex, urlPrefix, links, title, description);
        this.bbox = extent.flatMap(OgcApiExtent::getSpatial)
                     .map(OgcApiExtentSpatial::getBbox)
                     .map(v -> new BoundingBox(v[0][0], v[0][1], v[0][2], v[0][3], CRS84));
        this.temporalExtentIso = extent.flatMap(OgcApiExtent::getTemporal);
        this.temporalExtent = temporalExtentIso.map(v -> v.getInterval()[0])
                                          .map(v -> {
                                              ImmutableTemporalExtent.Builder builder = new ImmutableTemporalExtent.Builder();
                                              if (Objects.nonNull(v[0]))
                                                  builder.start(Instant.parse(v[0]).toEpochMilli());
                                              if (Objects.nonNull(v[1]))
                                                  builder.end(Instant.parse(v[1]).toEpochMilli());
                                              return builder.build();
                                          });
        this.uriCustomizer = uriCustomizer;
    }

    public abstract List<Link> getDistributionLinks();

    public List<Link> getLinks() {
        return links
                .stream()
                .filter(link -> !link.getRel().matches("^(?:self|alternate|data|tiles|styles|service-desc|service-doc|ogc-dapa-processes|ldp-map)$"))
                .collect(Collectors.toList());
    }

    public Optional<Metadata> getMetadata() {
        return apiData.getMetadata();
    }

    public Optional<String> getCanonicalUrl() {
        return links
                .stream()
                .filter(link -> Objects.equals(link.getRel(), "self"))
                .map(Link::getHref)
                .map(mayThrow(url -> new URICustomizer(url)
                        .clearParameters()
                        .ensureNoTrailingSlash()
                        .toString()))
                .findFirst();
    }

    public Map<String, String> getTemporalExtent() {
        if (temporalExtent.isEmpty())
            return null;
        else if (Objects.isNull(temporalExtent.get().getStart()) && Objects.isNull(temporalExtent.get().getEnd()))
            return ImmutableMap.of();
        else if (Objects.isNull(temporalExtent.get().getStart()))
            return ImmutableMap.of("end", String.valueOf(temporalExtent.get().getEnd()));
        else if (Objects.isNull(temporalExtent.get().getEnd()))
            return ImmutableMap.of("start", String.valueOf(temporalExtent.get().getStart()));
        else
            return ImmutableMap.of("start", String.valueOf(temporalExtent.get().getStart()), "end", String.valueOf(temporalExtent.get().getEnd()));
    }

    public Optional<String> getTemporalCoverage() {
        return temporalExtentIso.map(v -> v.getFirstIntervalIso8601());
    }

    public Map<String, String> getBbox() {
        return bbox.map(v -> ImmutableMap.of("minLng", Double.toString(v.getXmin()),
                                             "minLat", Double.toString(v.getYmin()),
                                             "maxLng", Double.toString(v.getXmax()),
                                             "maxLat", Double.toString(v.getYmax())))
                   .orElse(null);
    }

    public Optional<String> getSpatialCoverage() {
        return bbox.map(v -> String.format(Locale.US, "%f %f %f %f", v.getYmin(), v.getXmin(), v.getYmax(), v.getXmax()));
    }

    public Optional<String> getDistributionsAsString() {
        String distribution = getDistributionLinks().stream()
                .map(link -> "{ \"@type\": \"DataDownload\", " +
                        "\"name\":\"" + link.getTitle() + "\", " +
                        "\"encodingFormat\": \"" + link.getType() + "\", " +
                        "\"contentUrl\": \"" + link.getHref() + "\" }")
                .collect(Collectors.joining(", "));
        return distribution.isEmpty()
                ? Optional.empty()
                : Optional.of(distribution);
    }

    protected String getSchemaOrgDataset(OgcApiDataV2 apiData,
                                         Optional<FeatureTypeConfigurationOgcApi> collection,
                                         URICustomizer landingPageUriCustomizer,
                                         boolean embedded) {
        Metadata metadata = getMetadata().orElse(new ImmutableMetadata.Builder().build());
        String url = collection.map(s -> landingPageUriCustomizer.copy()
                                                                 .removeParameters()
                                                                 .ensureLastPathSegments("collections", s.getId())
                                                                 .ensureNoTrailingSlash()
                                                                 .toString())
                               .orElse(landingPageUriCustomizer.copy()
                                                               .removeParameters()
                                                               .ensureNoTrailingSlash()
                                                               .toString());

        return "{ " +
                (!embedded ? "\"@context\": \"https://schema.org/\", " : "") +
                "\"@type\": \"Dataset\", " +
                "\"name\": \"" + collection.map(FeatureTypeConfiguration::getLabel)
                                           .orElse(apiData.getLabel())
                                           .replace("\"", "\\\"") + "\", " +
                collection.map(FeatureTypeConfiguration::getDescription)
                          .orElse(apiData.getDescription())
                          .map(s -> "\"description\": \"" + s.replace("\"", "\\\"") + "\", ")
                          .orElse("") +
                (!embedded ? "\"keywords\": [ " + String.join(",", metadata.getKeywords()
                                                                              .stream()
                                                                              .filter(keyword -> Objects.nonNull(keyword) && !keyword.isEmpty())
                                                                              .map(keyword -> ("\"" + keyword + "\""))
                                                                              .collect(Collectors.toList())) + " ], " : "") +
                "\"creator\": { \"@type\": \"Organization\"" +
                metadata.getContactName()
                        .map(s -> ", \"name\": \""+s+"\"")
                        .orElse("") +
                metadata.getContactUrl()
                        .map(s -> ", \"url\": \""+s+"\"")
                        .orElse("") +
                (metadata.getContactEmail().isPresent() || metadata.getContactPhone().isPresent() ||metadata.getContactUrl().isPresent()
                        ? ", \"contactPoint\": { \"@type\": \"ContactPoint\", \"contactType\": \"technical support\"" +
                        metadata.getContactEmail()
                                .map(s -> ", \"email\": \""+s+"\"")
                                .orElse("") +
                        metadata.getContactPhone()
                                .map(s -> ", \"telephone\": \""+s+"\"")
                                .orElse("") +
                        metadata.getContactUrl()
                                .map(s -> ", \"url\": \""+s+"\"")
                                .orElse("") + "}"
                        : "") +
                "}, " +
                "\"url\":\"" + url + "\", " +
                "\"sameAs\":\"" + url + "\"" +
                (metadata.getLicenseUrl().isPresent() || metadata.getLicenseName().isPresent()
                        ? ", \"license\": { \"@type\": \"CreativeWork\"" + metadata.getLicenseName()
                                                                                   .map(s -> ", \"name\": \"" + s + "\"")
                                                                                   .orElse("") + metadata.getLicenseUrl()
                                                                                                         .map(s -> ", \"url\": \"" + s + "\"")
                                                                                                         .orElse("") + " }"
                        : "") +
                (!embedded
                        ? getTemporalCoverage().map(s -> ", \"temporalCoverage\": \"" + s + "\"")
                                                  .orElse("" )
                        : "") +
                (!embedded
                        ? getSpatialCoverage().map(s -> ", \"spatialCoverage\": { \"@type\": \"Place\", \"geo\":{ \"@type\": \"GeoShape\", \"box\": \"" + s + "\" } }")
                                                 .orElse("" )
                        : "") +
                (!embedded
                        ? collection.map(s -> ", \"isPartOf\": " + getSchemaOrgDataset(apiData, Optional.empty(), landingPageUriCustomizer.copy(), true))
                                    .orElse(", \"hasPart\": [ " + String.join(", ", apiData.getCollections()
                                                                                           .values()
                                                                                           .stream()
                                                                                           .map(s -> getSchemaOrgDataset(apiData, Optional.of(s), landingPageUriCustomizer.copy(), true))
                                                                                           .collect(Collectors.toUnmodifiableList())) + " ]" +
                                                    ", \"includedInDataCatalog\": { \"@type\": \"DataCatalog\", \"url\": \"" + landingPageUriCustomizer.copy()
                                                                                                                                                       .removeParameters()
                                                                                                                                                       .ensureNoTrailingSlash()
                                                                                                                                                       .toString()+ "\" }" )

                        : "") +
                (!embedded
                        ? getDistributionsAsString().map(s -> ", \"distribution\": [ " + s + " ]")
                                                    .orElse("")
                        : "") +
                "}";
    }
}
