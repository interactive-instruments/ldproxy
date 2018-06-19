/**
 * Copyright 2017 European Union, interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.rest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ldproxy.output.geojson.Gml2GeoJsonMappingProvider;
import de.ii.ldproxy.output.html.DatasetView;
import de.ii.ldproxy.output.html.FeatureCollectionView;
import de.ii.ldproxy.output.html.FeatureDTO;
import de.ii.ldproxy.output.html.FeaturePropertyDTO;
import de.ii.ldproxy.output.html.GetCapabilities2Dataset;
import de.ii.ldproxy.output.html.HtmlConfig;
import de.ii.ldproxy.output.html.NavigationDTO;
import de.ii.ldproxy.output.html.Wfs3DatasetView;
import de.ii.ldproxy.rest.internal.util.RangeHeader;
import de.ii.ldproxy.service.GetFeaturePaging;
import de.ii.ldproxy.service.LdProxyService;
import de.ii.ldproxy.service.SparqlAdapter;
import de.ii.ldproxy.target.geojson.FeatureTransformerGeoJson;
import de.ii.ldproxy.target.geojson.StreamingGml2GeoJsonFlow;
import de.ii.ldproxy.target.gml.FeatureTransformerGmlUpgrade;
import de.ii.ldproxy.target.html.FeatureTransformerHtml;
import de.ii.ldproxy.wfs3.LandingPage;
import de.ii.ldproxy.wfs3.URICustomizer;
import de.ii.ldproxy.wfs3.Wfs3Collection;
import de.ii.ldproxy.wfs3.Wfs3Collections;
import de.ii.ldproxy.wfs3.Wfs3ConformanceClasses;
import de.ii.ldproxy.wfs3.Wfs3Link;
import de.ii.ldproxy.wfs3.Wfs3LinksGenerator;
import de.ii.ldproxy.wfs3.Wfs3MediaTypes;
import de.ii.xsf.core.api.MediaTypeCharset;
import de.ii.xsf.core.api.Service;
import de.ii.xsf.core.api.exceptions.ResourceNotFound;
import de.ii.xsf.core.api.permission.AuthorizationProvider;
import de.ii.xsf.core.api.rest.ServiceResource;
import de.ii.xtraplatform.akka.http.AkkaHttp;
import de.ii.xtraplatform.auth.api.User;
import de.ii.xtraplatform.feature.query.api.FeatureQuery;
import de.ii.xtraplatform.feature.query.api.FeatureQueryBuilder;
import de.ii.xtraplatform.feature.query.api.FeatureStream;
import de.ii.xtraplatform.feature.transformer.api.AkkaStreamer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import de.ii.xtraplatform.feature.transformer.api.GmlConsumer;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.client.GetCapabilities;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSOperation;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSRequest;
import de.ii.xtraplatform.ogc.api.wfs.parser.LoggingWfsCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.MultiWfsCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesParser;
import io.dropwizard.auth.Auth;
import io.dropwizard.views.View;
import io.dropwizard.views.ViewRenderer;
import io.swagger.oas.annotations.Operation;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ConnectionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static de.ii.ldproxy.rest.internal.util.RangeHeader.parseRange;

/**
 * @author zahnen
 */
//@RolesAllowed("USER")
@PermitAll
@Produces({MediaTypeCharset.APPLICATION_JSON_UTF8, "application/geo+json"})
public class LdProxyServiceResource implements ServiceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdProxyServiceResource.class);
    protected LdProxyService service = null;

    protected UriInfo uriInfo;
    protected URICustomizer uriCustomizer;

    private String staticUrlPrefix = "";
    private Optional<URI> externalUri;
    private HtmlConfig htmlConfig;

    @Context
    protected void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
        this.uriCustomizer = new URICustomizer(uriInfo.getRequestUri());

        if (externalUri.isPresent()) {
            uriCustomizer.setScheme(externalUri.get()
                                               .getScheme());
            uriCustomizer.replaceInPath("/rest/services", externalUri.get()
                                                                     .getPath());

            this.staticUrlPrefix = new URICustomizer(uriInfo.getRequestUri())
                                                .cutPathAfterSegments("rest", "services")
                                                .replaceInPath("/rest/services", externalUri.get()
                                                                                            .getPath())
                                                .ensureTrailingSlash()
                                                .ensureLastPathSegment("___static___")
                                                .getPath();
        }
    }

    private URICustomizer getUriCustomizer() {
        return uriCustomizer.copy();
    }

    @Context
    protected HttpServletRequest request;

    private OpenApiResource openApiResource;
    private ViewRenderer mustacheRenderer;
    final private Wfs3LinksGenerator wfs3LinksGenerator;

    public LdProxyServiceResource() {
        this.wfs3LinksGenerator = new Wfs3LinksGenerator();
    }

    private AkkaStreamer akkaStreamer;
    private AkkaHttp akkaHttp;

    @Override
    public Service getService() {
        return service;
    }

    @Override
    public void setService(Service service) {
        this.service = (LdProxyService) service;
        if (openApiResource != null) {
            openApiResource.setService(this.service, getUriCustomizer().copy());
        }
    }

    public void inject(OpenApiResource openApiResource, AkkaStreamer akkaStreamer, AkkaHttp akkaHttp, String externalUrl, HtmlConfig htmlConfig) {
        this.openApiResource = openApiResource;

        URI externalUri = null;
        try {
            externalUri = new URI(externalUrl);
        } catch (URISyntaxException e) {
            // ignore
        }

        this.externalUri = Optional.ofNullable(externalUri);
        this.akkaStreamer = akkaStreamer;
        this.akkaHttp = akkaHttp;
        this.htmlConfig = htmlConfig;
    }

    @Override
    public void setMustacheRenderer(ViewRenderer mustacheRenderer) {
        this.mustacheRenderer = mustacheRenderer;
    }

    @Override
    public void init(AuthorizationProvider authorizationProvider) {

    }

    private void checkAuthentication(Optional<User> optionalUser) {
        if (service.isSecured() && !optionalUser.isPresent()) {
            throw new NotAuthorizedException("Bearer realm=\"ldproxy\"");
            //throw new ClientErrorException(Response.Status.UNAUTHORIZED);
        }
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public View getDatasetAsHtml1(@Auth Optional<User> optionalUser) throws URISyntaxException {
        checkAuthentication(optionalUser);

        return getDatasetAsHtml(false);
    }

    @Path("/collections")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public View getDatasetAsHtml2(@Auth Optional<User> optionalUser) throws URISyntaxException {
        checkAuthentication(optionalUser);

        return getDatasetAsHtml(true);
    }

    public View getDatasetAsHtml(/*@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user,@QueryParam("token") String token,*/ boolean isCollection) throws URISyntaxException {
        final Wfs3Collections wfs3Dataset = generateWfs3Dataset(Wfs3MediaTypes.HTML, Wfs3MediaTypes.JSON, Wfs3MediaTypes.XML);

        wfs3Dataset.getWfsCapabilities().title = service.getName();
        wfs3Dataset.getWfsCapabilities().description = service.getDescription();
        wfs3Dataset.getWfsCapabilities().url += "?SERVICE=WFS&REQUEST=GetCapabilities";

        final List<NavigationDTO> breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", getUriCustomizer().removeLastPathSegments(isCollection ? 2 : 1)
                                                                     .toString()))
                .add(new NavigationDTO(service.getName()))
                .build();

        return new Wfs3DatasetView(wfs3Dataset, breadCrumbs, staticUrlPrefix, htmlConfig);
    }

    @GET
    @Produces({MediaTypeCharset.APPLICATION_JSON_UTF8})
    public Response getDatasetAsJson(@Auth Optional<User> optionalUser, @QueryParam("callback") String callback) {
        checkAuthentication(optionalUser);

        return Response.ok()
                       .entity(new LandingPage(getUriCustomizer(), service, Wfs3MediaTypes.JSON, Wfs3MediaTypes.XML, Wfs3MediaTypes.HTML))
                       .header("Access-Control-Allow-Origin", "*")
                       .header("Access-Control-Allow-Methods", "GET")
                       .build();
    }

    @Path("/collections")
    @GET
    @Produces({MediaTypeCharset.APPLICATION_JSON_UTF8})
    public Response getDatasetAsJson2(@Auth Optional<User> optionalUser, @QueryParam("callback") String callback) {
        checkAuthentication(optionalUser);

        return Response.ok()
                       .entity(generateWfs3Dataset(Wfs3MediaTypes.JSON, Wfs3MediaTypes.XML, Wfs3MediaTypes.HTML))
                       .header("Access-Control-Allow-Origin", "*")
                       .header("Access-Control-Allow-Methods", "GET")
                       .build();
    }

    @GET
    @Produces({"application/xml;charset=utf-8"})
    public LandingPage getDatasetAsXml(@Auth Optional<User> optionalUser, @QueryParam("callback") String callback) {
        checkAuthentication(optionalUser);

        return new LandingPage(getUriCustomizer(), service, Wfs3MediaTypes.XML, Wfs3MediaTypes.JSON, Wfs3MediaTypes.HTML);
    }

    @Path("/collections")
    @GET
    @Produces({"application/xml;charset=utf-8"})
    public Wfs3Collections getCollectionsAsXml(@Auth Optional<User> optionalUser, @QueryParam("callback") String callback) {
        checkAuthentication(optionalUser);

        return generateWfs3Dataset(Wfs3MediaTypes.XML, Wfs3MediaTypes.JSON, Wfs3MediaTypes.HTML);
    }

    private Wfs3Collections generateWfs3Dataset(String mediaType, String... alternativeMediaTypes) {
        return new Wfs3Collections(getUriCustomizer(), service, mediaType, alternativeMediaTypes);
    }

    @Path("/api")
    @Operation
    public OpenApiResource getOpenApi() {
        return openApiResource;
    }

    @Path("/conformance")
    @GET
    @Produces({MediaTypeCharset.APPLICATION_JSON_UTF8})
    public Response getConformanceClasses(@Auth Optional<User> optionalUser) {
        checkAuthentication(optionalUser);

        return Response.ok()
                       .entity(new Wfs3ConformanceClasses())
                       .header("Access-Control-Allow-Origin", "*")
                       .header("Access-Control-Allow-Methods", "GET")
                       .build();
    }

    @Path("/conformance")
    @GET
    @Produces({"application/xml;charset=utf-8"})
    public Response getConformanceClassesAsXml(@Auth Optional<User> optionalUser) {
        checkAuthentication(optionalUser);

        return Response.ok()
                       .entity(new Wfs3ConformanceClasses())
                       .build();
    }

    private FeatureCollectionView createFeatureCollectionView(FeatureTypeConfiguration featureType) {
        URICustomizer uriBuilder = getUriCustomizer()
                .clearParameters()
                .ensureParameter("f", Wfs3MediaTypes.FORMATS.get(Wfs3MediaTypes.HTML))
                .ensureLastPathSegment("items");

        DatasetView dataset = new DatasetView("", uriInfo.getRequestUri(), null, staticUrlPrefix, htmlConfig);
        FeatureCollectionView featureTypeDataset = new FeatureCollectionView("featureCollection", uriInfo.getRequestUri(), featureType.getName(), featureType.getDisplayName(), staticUrlPrefix, htmlConfig);
        featureTypeDataset.temporalExtent = featureType.getTemporalExtent();
        featureTypeDataset.uriBuilder = uriBuilder;
        dataset.featureTypes.add(featureTypeDataset);

        WFSOperation operation = new GetCapabilities();

        WFSCapabilitiesAnalyzer analyzer = new MultiWfsCapabilitiesAnalyzer(
                new GetCapabilities2Dataset(dataset),
                new LoggingWfsCapabilitiesAnalyzer()
        );

        WFSCapabilitiesParser wfsParser = new WFSCapabilitiesParser(analyzer, service.staxFactory);

        wfsParser.parse(service.getWfsAdapter()
                               .request(operation));


        String[] b = featureTypeDataset.bbox.split(" ");
        String[] min = b[0].split(",");
        String[] max = b[1].split(",");

        featureTypeDataset.bbox2 = ImmutableMap.of("minLng", min[1], "minLat", min[0], "maxLng", max[1], "maxLat", max[0]);

        Map<String, String> htmlNames = service.getHtmlNamesForFeatureType(featureType);
        Set<Map.Entry<String, String>> filterFields = service.getFilterableFieldsForFeatureType(featureType, true)
                                                             .entrySet()
                                                             .stream()
                                                             .peek(entry -> {
                                                                 if (htmlNames.containsKey(entry.getValue())) {
                                                                     entry.setValue(htmlNames.get(entry.getValue()));
                                                                 }
                                                             })
                                                             .collect(Collectors.toSet());
        featureTypeDataset.filterFields = filterFields;

        return featureTypeDataset;
    }

    private FeatureCollectionView createFeatureDetailsView(FeatureTypeConfiguration featureType, String featureId, List<Wfs3Link> links) {
        FeatureCollectionView featureTypeDataset = new FeatureCollectionView("featureDetails", uriInfo.getRequestUri(), featureType.getName(), featureType.getDisplayName(), staticUrlPrefix, htmlConfig);
        featureTypeDataset.description = featureType.getDisplayName();

        URICustomizer uriBuilder = getUriCustomizer()
                .clearParameters()
                .ensureParameter("f", Wfs3MediaTypes.FORMATS.get(Wfs3MediaTypes.HTML))
                //.removePathSegment("items", -2)
                .removeLastPathSegments(1);

        featureTypeDataset.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", uriBuilder.copy()
                                                             .removePathSegment("collections", -3)
                                                             .removeLastPathSegments(3)
                                                             .toString()))
                .add(new NavigationDTO(service.getName(), uriBuilder.copy()
                                                                    .removePathSegment("collections", -3)
                                                                    .removeLastPathSegments(2)
                                                                    .toString()))
                .add(new NavigationDTO(featureType.getDisplayName(), uriBuilder.toString()))
                .add(new NavigationDTO(featureId))
                .build();

        featureTypeDataset.formats = links.stream()
                                          .filter(wfs3Link -> wfs3Link.rel.equals("alternate"))
                                          .map(wfs3Link -> new NavigationDTO(Wfs3MediaTypes.NAMES.get(wfs3Link.type), wfs3Link.href))
                                          .collect(Collectors.toList());

        /*new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("GeoJson", "f=json"))
                .add(new NavigationDTO("GML", "f=xml"))
                .add(new NavigationDTO("JSON-LD", "f=jsonld"))
                .build();*/

        return featureTypeDataset;
    }

    private void addDatasetNavigation(FeatureCollectionView featureCollectionView, FeatureTypeConfiguration featureType, List<Wfs3Link> links) {
        URICustomizer uriBuilder = getUriCustomizer()
                .clearParameters()
                .ensureParameter("f", Wfs3MediaTypes.FORMATS.get(Wfs3MediaTypes.HTML))
                .removePathSegment("items", -1)
                .removePathSegment("collections", -2);

        featureCollectionView.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", uriBuilder.copy()
                                                             .removeLastPathSegments(2)
                                                             .toString()))
                .add(new NavigationDTO(service.getName(), uriBuilder.copy()
                                                                    .removeLastPathSegments(1)
                                                                    .toString()))
                .add(new NavigationDTO(featureType.getDisplayName()))
                .build();

        // TODO: only activated formats
        featureCollectionView.formats = links.stream()
                                             .filter(wfs3Link -> wfs3Link.rel.equals("alternate"))
                                             .map(wfs3Link -> new NavigationDTO(Wfs3MediaTypes.NAMES.get(wfs3Link.type), wfs3Link.href))
                                             .collect(Collectors.toList());

        /*new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("GeoJson", "f=json"))
                .add(new NavigationDTO("GML", "f=xml"))
                .add(new NavigationDTO("JSON-LD", "f=jsonld"))
                .build();*/


    }

    private void addDatasetIndices(FeatureCollectionView featureCollectionView, FeatureTypeConfiguration featureType) {
        ImmutableList.Builder<NavigationDTO> indices = new ImmutableList.Builder<>();
        for (String index : service.findIndicesForFeatureType(featureType)
                                   .keySet()) {
            indices.add(new NavigationDTO(index, "?fields=" + index + "&distinctValues=true"));
        }
        featureCollectionView.indices = indices.build();

    }

    private void createIndexPage(FeatureCollectionView featureCollectionView, FeatureTypeConfiguration featureType, String fields) {
        featureCollectionView.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                .add(new NavigationDTO("Datasets", "../../"))
                .add(new NavigationDTO(service.getName(), "../../" + service.getBrowseUrl()))
                .add(new NavigationDTO(featureType.getDisplayName(), "./"))
                .add(new NavigationDTO(fields))
                .build();

        featureCollectionView.formats = new ImmutableList.Builder<NavigationDTO>()
                .build();

        featureCollectionView.hideMetadata = true;
        featureCollectionView.hideMap = true;
        featureCollectionView.index = fields;
        List<String> values = service.getIndexValues(featureType, fields, service.findIndicesForFeatureType(featureType)
                                                                                 .get(fields));
        for (int i = 0; i < values.size(); i++) {
            FeatureDTO currentFeature = new FeatureDTO();
            currentFeature.name = values.get(i);
            currentFeature.id = new FeaturePropertyDTO();
            currentFeature.id.value = "?" + fields + "=" + values.get(i);
            currentFeature.noUrlClosingSlash = true;
            featureCollectionView.features.add(currentFeature);
        }
    }

    private void createFilteredByIndexPage(FeatureCollectionView featureCollectionView, FeatureTypeConfiguration featureType, String filterKey, String filterValue) {
        // + TODO: same for postalCode
        // + check template url + isPartOf TODO: schema.org mapping of aggregations and links
        // + TODO: harvest all values
        // TODO: links in json-ld
        // TODO: view as for aggregation sites

        featureCollectionView.hideMetadata = true;
        featureCollectionView.index = filterKey;
        featureCollectionView.indexValue = filterValue;

        Map<String, String> announcements = null;
        if (filterKey.equals("addressLocality")) {
            announcements = service.getSparqlAdapter()
                                   .request(filterValue, SparqlAdapter.QUERY.ADDRESS_LOCALITY);
        } else if (filterKey.equals("postalCode")) {
            announcements = service.getSparqlAdapter()
                                   .request(filterValue, SparqlAdapter.QUERY.POSTAL_CODE);
        }

        // TODO
        if (announcements != null && !announcements.isEmpty()) {
            featureCollectionView.links = new FeaturePropertyDTO();
            featureCollectionView.links.name = "Announcements";
            for (Map.Entry<String, String> id : announcements.entrySet()) {
                FeaturePropertyDTO link = new FeaturePropertyDTO();
                link.value = id.getKey();
                link.name = makeDisplayName(id.getKey(), id.getValue());
                link.itemProp = id.getValue();
                featureCollectionView.links.addChild(link);
            }
        }
    }

    private void addFilteredByIndexNavigation(FeatureCollectionView featureCollectionView, FeatureTypeConfiguration featureType, String filterKey, String filterValue) {
        if (service.findIndicesForFeatureType(featureType)
                   .containsKey(filterKey)) {
            featureCollectionView.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                    .add(new NavigationDTO("Datasets", "../../"))
                    .add(new NavigationDTO(service.getName(), "../../" + service.getBrowseUrl()))
                    .add(new NavigationDTO(featureType.getDisplayName(), "./"))
                    .add(new NavigationDTO(filterKey, "./?fields=" + filterKey + "&distinctValues=true"))
                    .add(new NavigationDTO(filterValue))
                    .build();
        } else {
            featureCollectionView.breadCrumbs = new ImmutableList.Builder<NavigationDTO>()
                    .add(new NavigationDTO("Datasets", "../../"))
                    .add(new NavigationDTO(service.getName(), "../../" + service.getBrowseUrl()))
                    .add(new NavigationDTO(featureType.getDisplayName(), "./"))
                    .add(new NavigationDTO(filterKey))
                    .add(new NavigationDTO(filterValue))
                    .build();
        }
    }

    @Path("/collections/{layerid}")
    @GET
    @Produces(MediaTypeCharset.TEXT_HTML_UTF8)
    public Response getFeaturesAsHtml1(@Auth Optional<User> optionalUser, @PathParam("layerid") String layerid, @QueryParam("fields") String fields, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {
        checkAuthentication(optionalUser);

        return getFeaturesAsHtml(layerid, fields, callback, range);
    }

    @Path("/collections/{layerid}/items")
    @GET
    @Produces(MediaTypeCharset.TEXT_HTML_UTF8)
    public Response getFeaturesAsHtml2(@Auth Optional<User> optionalUser, @PathParam("layerid") String layerid, @QueryParam("fields") String fields, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {
        checkAuthentication(optionalUser);

        return getFeaturesAsHtml(layerid, fields, callback, range);
    }

    public Response getFeaturesAsHtml(String layerid, String fields, String callback, String range) {

        FeatureTypeConfiguration featureType = getFeatureTypeForLayerId(layerid);

        LOGGER
                .debug("GET HTML FOR {} {}", featureType.getNamespace(), featureType.getName());

        int[] r = RangeHeader.parseRange(range);
        List<Wfs3Link> links = wfs3LinksGenerator.generateCollectionOrFeatureLinks(getUriCustomizer(), true, r[2], r[3], Wfs3MediaTypes.HTML, Wfs3MediaTypes.GEO_JSON, Wfs3MediaTypes.GML);


        FeatureCollectionView featureTypeDataset = createFeatureCollectionView(featureType);

        addDatasetNavigation(featureTypeDataset, featureType, links);

        addDatasetIndices(featureTypeDataset, featureType);


        List<String> groupings = new ArrayList<>();
        if (layerid.equals("inspireadressen")) {
            groupings.add("woonplaats");
        }

        String query = request.getQueryString() == null ? "" : request.getQueryString();


        // TODO: split fields
        if (fields != null && service.findIndicesForFeatureType(featureType)
                                     .containsKey(fields)) {

            createIndexPage(featureTypeDataset, featureType, fields);

            return Response.ok(featureTypeDataset)
                           .build();
        }

        FeatureQuery featureQuery = getWfsFeaturesPagedAndFiltered(featureType, range, ((filterKey, filterValue) -> {
            createFilteredByIndexPage(featureTypeDataset, featureType, filterKey, filterValue);
            addFilteredByIndexNavigation(featureTypeDataset, featureType, filterKey, filterValue);
        }));

        //if (indexId.toLowerCase().equals("all")) {
        return getHtmlResponse(featureQuery, featureType, true, groupings, false, query, parseRange(range), featureTypeDataset);
        //} else {
        //    return getWfsPropertiesPaged(layerid, range, indexId);
        //}
    }

    public interface FilterConsumer {
        void with(String filterKey, String filterValue);
    }

    private FeatureQuery getWfsHitsFiltered(FeatureTypeConfiguration featureType) {

        final Map<String, String> filterableFields = service.getFilterableFieldsForFeatureType(featureType);

        final Map<String, String> filters = getFiltersFromQuery(uriInfo.getQueryParameters(), filterableFields);

        //return getWfsHits(featureType, filters, filterableFields);

        final FeatureQueryBuilder queryBuilder = new FeatureQueryBuilder().type(featureType.getName());

        if (!filters.isEmpty()) {
            String cql = getCQLFromFilters(filters, filterableFields);
            LOGGER.debug("CQL {}", cql);
            queryBuilder.filter(cql);
        }

        return queryBuilder.build();
    }

    private FeatureQuery getWfsFeaturesPagedAndFiltered(FeatureTypeConfiguration featureType, String range) {
        return getWfsFeaturesPagedAndFiltered(featureType, range, null);
    }

    private FeatureQuery getWfsFeaturesPagedAndFiltered(FeatureTypeConfiguration featureType, String range, FilterConsumer indexFilterConsumer) {
        WFSOperation wfsOperation = null;

        final Map<String, String> filterableFields = service.getFilterableFieldsForFeatureType(featureType);

        final Map<String, String> indexFilterableFields = service.findIndicesForFeatureType(featureType, false);

        final Map<String, String> filters = getFiltersFromQuery(uriInfo.getQueryParameters(), filterableFields, indexFilterableFields, indexFilterConsumer);

        final int[] countFrom = parseRange(range);

        final FeatureQueryBuilder queryBuilder = new FeatureQueryBuilder().type(featureType.getName())
                                                                          .limit(countFrom[0])
                                                                          .offset(countFrom[1]);

        if (!filters.isEmpty()) {
            wfsOperation = getWfsFeaturesPaged(featureType.getName(), range, filters, filterableFields);
            String cql = getCQLFromFilters(filters, filterableFields);
            LOGGER.debug("CQL {}", cql);
            queryBuilder.filter(cql);
        } else {
            wfsOperation = getWfsFeaturesPaged(featureType.getName(), range);
        }
        return queryBuilder.build();
    }

    private Map<String, String> getFiltersFromQuery(MultivaluedMap<String, String> query, Map<String, String> filterableFields) {
        return getFiltersFromQuery(query, filterableFields, null, null);
    }

    private Map<String, String> getFiltersFromQuery(MultivaluedMap<String, String> query, Map<String, String> filterableFields, Map<String, String> indexFilterableFields, FilterConsumer indexFilterConsumer) {

        Map<String, String> filters = new LinkedHashMap<>();

        for (String filterKey : query.keySet()) {
            if (filterableFields.containsKey(filterKey.toLowerCase())) {
                String filterValue = query.getFirst(filterKey);
                filters.put(filterKey.toLowerCase(), filterValue);
            } else if (indexFilterableFields != null && indexFilterableFields.containsKey(filterKey)) {

                String filterValue = query.getFirst(filterKey);

                if (indexFilterConsumer != null) {
                    indexFilterConsumer.with(filterKey, filterValue);
                }

                String indexId = indexFilterableFields.get(filterKey);
                indexId = indexId.substring(indexId.lastIndexOf(':') + 1);

                filters.put(indexId, filterValue);
            }
        }

        return filters;
    }

    private String getCQLFromFilters(Map<String, String> filters, Map<String, String> filterableFields) {
        return filters.entrySet()
                      .stream()
                      .map(f -> {
                          if (f.getKey()
                               .equals("bbox")) {
                              String[] bbox = f.getValue()
                                               .split(",");
                              String[] bbox2 = {bbox[1], bbox[0], bbox[3], bbox[2]};
                              return String.format("BBOX(%s, %s)", filterableFields.get(f.getKey()), Joiner.on(',')
                                                                                                           .join(bbox2));
                          }
                          if (f.getKey()
                               .equals("time")) {
                              try {
                                  Interval fromIso8601Period = Interval.parse(f.getValue());
                                  return String.format("%s DURING %s", filterableFields.get(f.getKey()), fromIso8601Period);
                              } catch (DateTimeParseException ignore) {
                                  try {
                                      Instant fromIso8601 = Instant.parse(f.getValue());
                                      return String.format("%s TEQUALS %s", filterableFields.get(f.getKey()), fromIso8601);
                                  } catch (DateTimeParseException e) {
                                      LOGGER.debug("TIME PARSER ERROR", e);
                                      throw new BadRequestException();
                                  }
                              }
                          }
                          if (f.getValue()
                               .contains("*")) {
                              return String.format("%s LIKE '%s'", filterableFields.get(f.getKey()), f.getValue());
                          }

                          return String.format("%s = %s", filterableFields.get(f.getKey()), f.getValue());
                      })
                      .collect(Collectors.joining(" AND "));
    }

    private String makeDisplayName(String id, String title) {
        if (title.length() > 100) {
            title = title.substring(0, 99) + " ...";
        }
        return title + " (" + id.substring(id.lastIndexOf('/') + 1) + ")";
    }

    @Path("/collections/{layerid}")
    @GET
    public Response getCollectionInfo(@Auth Optional<User> optionalUser, @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @QueryParam("callback") String callback, @QueryParam("resultType") String resultType, @HeaderParam("Range") String range) {
        checkAuthentication(optionalUser);

        final Optional<Wfs3Collection> wfs3Collection = generateWfs3Dataset(Wfs3MediaTypes.JSON, Wfs3MediaTypes.XML, Wfs3MediaTypes.HTML).getCollections()
                                                                                                                                             .stream()
                                                                                                                                             .filter(collection -> collection.getName()
                                                                                                                                                                             .equals(layerid))
                                                                                                                                             .findFirst();

        if (!wfs3Collection.isPresent()) {
            throw new ResourceNotFound();
        }

        //final int[] r = RangeHeader.parseRange(range);
        //final List<Wfs3Link> links = wfs3LinksGenerator.generateCollectionOrFeatureLinks(uriInfo.getRequestUri(), true, r[2], r[3], Wfs3MediaTypes.GEO_JSON, Wfs3MediaTypes.GML, Wfs3MediaTypes.HTML);

        //wfs3Collection.get().setLinks(links);

        return Response.ok()
                       .entity(wfs3Collection.get())
                       .header("Access-Control-Allow-Origin", "*")
                       .header("Access-Control-Allow-Methods", "GET")
                       .build();

        //return getFeaturesAsJson(layerid, indexId, callback, resultType, range);
    }

    @Path("/collections/{layerid}/items")
    @GET
    public Response getFeaturesAsJson2(@Auth Optional<User> optionalUser, @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @QueryParam("callback") String callback, @QueryParam("resultType") String resultType, @HeaderParam("Range") String range) {
        checkAuthentication(optionalUser);

        return getFeaturesAsJson(layerid, indexId, callback, resultType, range);
    }

    public Response getFeaturesAsJson(String layerid, String indexId, String callback, String resultType, String range) {

        FeatureTypeConfiguration featureType = getFeatureTypeForLayerId(layerid);

        LOGGER
                .debug("GET JSON FOR {} {}", featureType.getNamespace(), featureType.getName());

        if (resultType != null && resultType.equals("hits")) {
            return getJsonHits(getWfsHitsFiltered(featureType), featureType);
        }

        //if (indexId.toLowerCase().equals("all")) {
        return getJsonResponse(getWfsFeaturesPagedAndFiltered(featureType, range), featureType, true, range, callback);
        //} else {
        //    return getWfsPropertiesPaged(layerid, range, indexId);
        //}
    }

    //TODO: finish async tests
    @Path("/{layerid}/async")
    @GET
    public void getFeaturesAsJsonAsync(@Suspended AsyncResponse asyncResponse, @Auth Optional<User> optionalUser, @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @QueryParam("callback") String callback, @QueryParam("resultType") String resultType, @HeaderParam("Range") String range) throws IOException {
        checkAuthentication(optionalUser);


        FeatureTypeConfiguration featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.debug("GET ASNYC JSON FOR {} {}", featureType.getNamespace(), featureType.getName());

        getJsonResponseAsync(getWfsFeaturesPagedAndFiltered(featureType, range), featureType, true, range, callback, asyncResponse);
    }

    @Path("/collections/{layerid}/async")
    @GET
    @ManagedAsync
    public void getFeaturesAsJsonAsync2(@Suspended AsyncResponse asyncResponse, @Auth Optional<User> optionalUser, @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @QueryParam("callback") String callback, @QueryParam("resultType") String resultType, @HeaderParam("Range") String range) throws IOException {
        checkAuthentication(optionalUser);


        FeatureTypeConfiguration featureType = getFeatureTypeForLayerId(layerid);

        LOGGER.debug("GET ASNYC JSON FOR {} {}", featureType.getNamespace(), featureType.getName());

        if (resultType != null && resultType.equals("hits")) {
            asyncResponse.resume(getJsonHits(getWfsHitsFiltered(featureType), featureType));
        }

        getJsonResponseAsync2(getWfsFeaturesPagedAndFiltered(featureType, range), featureType, true, range, callback, asyncResponse);
    }

    @Path("/collections/{layerid}")
    @GET
    @Produces("application/ld+json;charset=utf-8")
    public Response getFeaturesAsJsonLd1(@Auth Optional<User> optionalUser, @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {
        checkAuthentication(optionalUser);

        return getFeaturesAsJsonLd(layerid, indexId, callback, range);
    }

    @Path("/collections/{layerid}/items")
    @GET
    @Produces("application/ld+json;charset=utf-8")
    public Response getFeaturesAsJsonLd2(@Auth Optional<User> optionalUser, @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {
        checkAuthentication(optionalUser);

        return getFeaturesAsJsonLd(layerid, indexId, callback, range);
    }

    public Response getFeaturesAsJsonLd(String layerid, String indexId, String callback, String range) {

        FeatureTypeConfiguration featureType = getFeatureTypeForLayerId(layerid);

        LOGGER
                .debug("GET JSON-LD FOR {} {}", featureType.getNamespace(), featureType.getName());

        FeatureCollectionView featureTypeDataset = createFeatureCollectionView(featureType);

        FeatureQuery featureQuery = getWfsFeaturesPagedAndFiltered(featureType, range, ((filterKey, filterValue) -> createFilteredByIndexPage(featureTypeDataset, featureType, filterKey, filterValue)));

        //if (indexId.toLowerCase().equals("all")) {
        return getJsonLdResponse(featureQuery, featureType, true, featureTypeDataset);
        //} else {
        //    return getWfsPropertiesPaged(layerid, range, indexId);
        //}
    }

    @Path("/collections/{layerid}")
    @GET
    @Produces({"application/xml;charset=utf-8", "application/gml+xml;version=3.2"})
    public Response getCollectionInfoAsXml(@Auth Optional<User> optionalUser, @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @QueryParam("callback") String callback, @QueryParam("resultType") String resultType, @HeaderParam("Range") String range) {
        checkAuthentication(optionalUser);

        final List<Wfs3Collection> wfs3Collection = generateWfs3Dataset(Wfs3MediaTypes.XML, Wfs3MediaTypes.JSON, Wfs3MediaTypes.HTML).getCollections()
                                                                                                                                     .stream()
                                                                                                                                     .filter(collection -> collection.getName()
                                                                                                                                                                         .equals(layerid))
                                                                                                                                     .collect(Collectors.toList());

        if (wfs3Collection.isEmpty()) {
            throw new ResourceNotFound();
        }

        return Response.ok()
                       .entity(new Wfs3Collections(wfs3Collection, new ArrayList<>()))
                       .build();
    }

    @Path("/collections/{layerid}/items")
    @GET
    @Produces({"application/xml;charset=utf-8", "application/gml+xml;version=3.2"})
    public Response getFeaturesAsXml2(@Auth Optional<User> optionalUser, @PathParam("layerid") String layerid, @QueryParam("properties") String fields, @QueryParam("callback") String callback, @QueryParam("resultType") String resultType, @HeaderParam("Range") String range) {
        checkAuthentication(optionalUser);

        return getFeaturesAsXml(layerid, fields, callback, resultType, range);
    }

    public Response getFeaturesAsXml(String layerid, String fields, String callback, String resultType, String range) {

        FeatureTypeConfiguration featureType = getFeatureTypeForLayerId(layerid);

        LOGGER
                .debug("GET XML FOR {} {}", featureType.getNamespace(), featureType.getName());

        int[] r = RangeHeader.parseRange(range);
        List<Wfs3Link> links = wfs3LinksGenerator.generateCollectionOrFeatureLinks(getUriCustomizer(), true, r[2], r[3], Wfs3MediaTypes.GML, Wfs3MediaTypes.GEO_JSON, Wfs3MediaTypes.HTML);


        GmlConsumerBuilder transformer = outputStream -> new FeatureTransformerGmlUpgrade(outputStream, true, service.getWfsAdapter()
                                                                                                                     .getNsStore()
                                                                                                                     .getNamespaces(), service.getCrsTransformations()
                                                                                                                                              .getDefaultTransformer(), links, r[3]);

        if (resultType != null && resultType.equals("hits")) {
            return getWfsResponse(getWfsHitsFiltered(featureType), transformer);
        }

        //if (indexId.toLowerCase().equals("all")) {
        return getWfsResponse(getWfsFeaturesPagedAndFiltered(featureType, range), transformer);
        //} else {
        //    return getWfsPropertiesPaged(layerid, range, indexId);
        //}
    }

    @Path("/collections/{layerid}/items/{featureid}")
    @GET
    public Response getFeatureByIdAsJson(@Auth Optional<User> optionalUser, @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @PathParam("featureid") final String featureid, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {
        checkAuthentication(optionalUser);


        FeatureTypeConfiguration featureType = getFeatureTypeForLayerId(layerid);

        LOGGER
                .debug("GET JSON FOR {} {}", featureType.getNamespace(), featureType.getName());

        //if (indexId.toLowerCase().equals("all")) {
        return getJsonResponse(getWfsFeatureById(layerid, featureid), featureType, false);
        //} else {
        //    return getWfsFeaturesPaged(layerid, range, indexId, featureid);
        //}
    }

    @Path("/collections/{layerid}/items/{featureid}")
    @GET
    @Produces("application/ld+json;charset=utf-8")
    public Response getFeatureByIdAsJsonLd(@Auth Optional<User> optionalUser, @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @PathParam("featureid") final String featureid, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {
        checkAuthentication(optionalUser);


        FeatureTypeConfiguration featureType = getFeatureTypeForLayerId(layerid);

        LOGGER
                .debug("GET JSON-LD FOR {} {}", featureType.getNamespace(), featureType.getName());

        //if (indexId.toLowerCase().equals("all")) {
        return getJsonLdResponse(getWfsFeatureById(layerid, featureid), featureType, false, null);
        //} else {
        //    return getWfsFeaturesPaged(layerid, range, indexId, featureid);
        //}
    }

    @Path("/collections/{layerid}/items/{featureid}")
    @GET
    @Produces({"application/xml;charset=utf-8", "application/gml+xml;version=3.2"})
    public Response getFeatureByIdAsAXml(@Auth Optional<User> optionalUser, @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @PathParam("featureid") final String featureid, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {
        checkAuthentication(optionalUser);


        FeatureTypeConfiguration featureType = getFeatureTypeForLayerId(layerid);

        LOGGER
                .debug("GET XML FOR {} {}", featureType.getNamespace(), featureType.getName());

        int[] r = RangeHeader.parseRange(range);
        List<Wfs3Link> links = wfs3LinksGenerator.generateCollectionOrFeatureLinks(getUriCustomizer(), false, r[2], r[3], Wfs3MediaTypes.GML, Wfs3MediaTypes.GEO_JSON, Wfs3MediaTypes.HTML);

        GmlConsumerBuilder transformer = outputStream -> new FeatureTransformerGmlUpgrade(outputStream, false, service.getWfsAdapter()
                                                                                                                      .getNsStore()
                                                                                                                      .getNamespaces(), service.getCrsTransformations()
                                                                                                                                               .getDefaultTransformer(), links, r[3]);


        //if (indexId.toLowerCase().equals("all")) {
        return getWfsResponse(getWfsFeatureById(layerid, featureid), transformer);
        //} else {
        //    return getWfsFeaturesPaged(layerid, range, indexId, featureid);
        //}
    }

    @Path("/collections/{layerid}/items/{featureid}")
    @GET
    @Produces(MediaTypeCharset.TEXT_HTML_UTF8)
    public Response getFeatureByIdAsHtml(@Auth Optional<User> optionalUser, @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @PathParam("featureid") final String featureid, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {
        checkAuthentication(optionalUser);

        FeatureTypeConfiguration featureType = getFeatureTypeForLayerId(layerid);

        LOGGER
                .debug("GET HTML FOR {} {}", featureType.getNamespace(), featureType.getName());

        List<Wfs3Link> links = wfs3LinksGenerator.generateCollectionOrFeatureLinks(getUriCustomizer(), false, 0, 0, Wfs3MediaTypes.HTML, Wfs3MediaTypes.GEO_JSON, Wfs3MediaTypes.GML);

        FeatureCollectionView featureTypeDataset = createFeatureDetailsView(featureType, featureid, links);

        //if (indexId.toLowerCase().equals("all")) {
        return getHtmlResponse(getWfsFeatureById(layerid, featureid), featureType, false, new ArrayList<>(), false, "", null, featureTypeDataset);
        //} else {
        //    return getWfsFeaturesPaged(layerid, range, indexId, featureid);
        //}
    }

    /*@Path("/{layerid}/{indexId}/{featureid}/{featureid2}")
    @GET
    @Produces("application/xml;charset=utf-8")
    public Response getServiceLayerFeature2GET(@Auth(protectedResource = true, exceptions = "arcgis") AuthenticatedUser user, @PathParam("layerid") String layerid, @PathParam("indexId") String indexId, @PathParam("featureid") final String featureid, @PathParam("featureid2") final String featureid2, @QueryParam("callback") String callback, @HeaderParam("Range") String range) {

        // TODO: filter, error handling
            return getWfsResponse(getWfsFeatureById(layerid, featureid2));
    }*/


    /*private WFSOperation getWfsHits(WfsProxyFeatureType featureType, Map<String, String> filterValues, Map<String, String> filterPaths) {
        return new GetFeatureHits(featureType.getNamespace(), featureType.getName(), filterValues, filterPaths);
    }*/

    private WFSOperation getWfsFeaturesPaged(String layerId, String range, Map<String, String> filterValues, Map<String, String> filterPaths) {
        FeatureTypeConfiguration featureType = getFeatureTypeForLayerId(layerId);
        int[] countFrom = parseRange(range);
        int count = countFrom[0];
        int startIndex = countFrom[1];

        return new GetFeaturePaging(featureType.getNamespace(), featureType.getName(), count, startIndex, filterValues, filterPaths);
    }

    private WFSOperation getWfsFeaturesPaged(String layerId, String range) {
        FeatureTypeConfiguration featureType = getFeatureTypeForLayerId(layerId);
        int[] countFrom = parseRange(range);
        int count = countFrom[0];
        int startIndex = countFrom[1];

        return new GetFeaturePaging(featureType.getNamespace(), featureType.getName(), count, startIndex);
    }

    private FeatureQuery getWfsFeatureById(String layerId, String featureId) {
        FeatureTypeConfiguration featureType = getFeatureTypeForLayerId(layerId);

        return new FeatureQueryBuilder().type(featureType.getName())
                                        .filter(String.format("IN ('%s')", featureId))
                                        .build();
        //return new GetFeatureById(featureType.getNamespace(), featureType.getName(), featureId);
    }

    /*private Response getWfsDescribeFeatureType(String layerId) {
        String[] ft = parseLayerId(layerId);

        Map<String, List<String>> fts = new ImmutableMap.Builder<String, List<String>>().put(ft[0], Lists.newArrayList(ft[1])).build();
        LOGGER.debug("GET LAYER {}", fts);
        WFSOperation operation = new DescribeFeatureType(fts);

        return getWfsResponse(operation);
    }*/

    /*private Response getWfsCapabilities() {
        WFSOperation operation = new GetCapabilities();

        return getWfsResponse(operation);
    }*/

    /*private WFSOperation getWfsPropertiesPaged(String layerId, String range, String indexId) {
        String[] ft = parseLayerId(layerId);
        int[] countFrom = parseRange(range);
        int count = countFrom[0];
        int startIndex = countFrom[1];

        return new GetPropertyValuePaging(ft[0], ft[1], indexId, count, startIndex);
    }*/

    public interface GmlAnalyzerBuilder {
        GMLAnalyzer with(OutputStream outputStream) throws IOException;
    }

    public interface FeatureTransformerBuilder {
        FeatureTransformer with(OutputStream outputStream) throws IOException;
    }

    public interface GmlConsumerBuilder {
        GmlConsumer with(OutputStream outputStream) throws IOException;
    }

    public interface WFSRequestConsumer {
        void with(WFSRequest request);
    }

    private Response getJsonResponse(final FeatureQuery featureQuery, final FeatureTypeConfiguration featureType, final boolean isFeatureCollection) {
        return getJsonResponse(featureQuery, featureType, isFeatureCollection, null, null);
    }

    private Response getJsonResponse(final FeatureQuery featureQuery, final FeatureTypeConfiguration featureType, final boolean isFeatureCollection, String range, String callback) {

        int[] r = RangeHeader.parseRange(range);
        List<Wfs3Link> links = wfs3LinksGenerator.generateCollectionOrFeatureLinks(getUriCustomizer(), isFeatureCollection, r[2], r[3], Wfs3MediaTypes.GEO_JSON, Wfs3MediaTypes.GML, Wfs3MediaTypes.HTML);

        /*return getResponse(featureQuery, featureType,
                outputStream -> new GeoJsonFeatureWriter(service.createJsonGenerator(outputStream), service.jsonMapper, isFeatureCollection, featureType.getMappings(), Gml2GeoJsonMappingProvider.MIME_TYPE, service.getCrsTransformations()
                                                                                                                                                                                                                     .getDefaultTransformer(), links), range, callback, false)
                .type("application/geo+json")
                .build();*/

        return getResponse(featureQuery, outputStream -> new FeatureTransformerGeoJson(service.createJsonGenerator(outputStream), isFeatureCollection, service.getCrsTransformations()
                                                                                                                                                              .getDefaultTransformer(), links, r[3]))
                .type("application/geo+json")
                .build();
    }

    private void getJsonResponseAsync(final FeatureQuery featureQuery, final FeatureTypeConfiguration featureType, final boolean isFeatureCollection, String range, String callback, AsyncResponse asyncResponse) throws IOException {

        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                if (throwable == null) {
                    //Everything is good. Response has been successfully
                    //dispatched to client
                    LOGGER.debug("ASYNC SUCCESS");
                } else {
                    //An error has occurred during request processing
                    LOGGER.debug("ASYNC FAIL");
                }
            }
        }, new ConnectionCallback() {
            public void onDisconnect(AsyncResponse disconnected) {
                //Connection lost or closed by the client!
                LOGGER.debug("DISCONNECT");
            }
        });

        final Consumer<StreamingOutput> onSuccess = streamingOutput -> {
            LOGGER.debug("RESUME");

            asyncResponse.resume(
                    Response.ok()
                            .entity(streamingOutput)
                            .header("Access-Control-Allow-Origin", "*")
                            .header("Access-Control-Allow-Methods", "GET")
                            .build()
            );
        };

        akkaStreamer.stream(featureType, service.getFeatureProvider()
                                                .encodeFeatureQuery(featureQuery), Gml2GeoJsonMappingProvider.MIME_TYPE, isFeatureCollection, () -> StreamingGml2GeoJsonFlow.transformer(new QName(featureType.getNamespace(), featureType.getName()), featureType.getMappings()), onSuccess, throwable -> {
            asyncResponse.resume(throwable);
            return null;
        });


    }

    private void getJsonResponseAsync2(final FeatureQuery featureQuery, final FeatureTypeConfiguration featureType, final boolean isFeatureCollection, String range, String callback, AsyncResponse asyncResponse) throws IOException {

        asyncResponse.register(new CompletionCallback() {
            @Override
            public void onComplete(Throwable throwable) {
                if (throwable == null) {
                    //Everything is good. Response has been successfully
                    //dispatched to client
                    LOGGER.debug("ASYNC SUCCESS");
                } else {
                    //An error has occurred during request processing
                    LOGGER.debug("ASYNC FAIL");
                }
            }
        }, new ConnectionCallback() {
            public void onDisconnect(AsyncResponse disconnected) {
                //Connection lost or closed by the client!
                LOGGER.debug("DISCONNECT");
            }
        });

        final Consumer<StreamingOutput> onSuccess = streamingOutput -> {
            LOGGER.debug("RESUME");

            asyncResponse.resume(
                    Response.ok()
                            .entity(streamingOutput)
                            .header("Access-Control-Allow-Origin", "*")
                            .header("Access-Control-Allow-Methods", "GET")
                            .build()
            );
        };

        akkaStreamer.streamFeatures(featureType, service.getFeatureProvider()
                                                        .encodeFeatureQuery(featureQuery), Gml2GeoJsonMappingProvider.MIME_TYPE, isFeatureCollection, () -> StreamingGml2GeoJsonFlow.transformer(new QName(featureType.getNamespace(), featureType.getName()), featureType.getMappings()), onSuccess, throwable -> {
            asyncResponse.resume(throwable);
            return null;
        });


    }

    private Response getJsonHits(final FeatureQuery featureQuery, final FeatureTypeConfiguration featureType) {
        return Response.noContent()
                       .build();
        //return getResponse(featureQuery, featureType,
        //        outputStream -> new GeoJsonHitsWriter(service.createJsonGenerator(outputStream), service.jsonMapper, true, service.getCrsTransformations()
        //                                                                                                                          .getDefaultTransformer()), true)
        //        .build();
    }

    private Response getJsonLdResponse(final FeatureQuery featureQuery, final FeatureTypeConfiguration featureType, final boolean isFeatureCollection, final FeatureCollectionView dataset) {
        return Response.noContent()
                       .build();
        //return getResponse(featureQuery, featureType,
        //        outputStream -> new JsonLdOutputWriter(service.createJsonGenerator(outputStream), service.jsonMapper, isFeatureCollection, featureType.getMappings(), Gml2JsonLdMappingProvider.MIME_TYPE, service.getCrsTransformations()
        //                                                                                                                                                                                                          .getDefaultTransformer(), uriInfo.getRequestUri(), dataset, ImmutableMap.of(), service.getVocab()))
        //        .build();
    }

    private Response getHtmlResponse(final FeatureQuery featureQuery, final FeatureTypeConfiguration featureType, final boolean isFeatureCollection, final List<String> groupings, final boolean group, final String query, final int[] range, final FeatureCollectionView featureTypeDataset) {

        //return getResponse(featureQuery, featureType,
        //        outputStream -> new MicrodataFeatureWriter(new OutputStreamWriter(outputStream), featureType.getMappings(), Gml2MicrodataMappingProvider.MIME_TYPE, isFeatureCollection, featureType.getName()
        //                                                                                                                                                                                            .equals("inspireadressen"), groupings, group, query, range, featureTypeDataset, service.getCrsTransformations()
        //                                                                                                                                                                                                                                                                                   .getDefaultTransformer(), service.getSparqlAdapter(), service.getCodelistStore(), mustacheRenderer)
        // TODO: this was used for sameAs microdata on single feature pages
        // is this still needed? then something like getFeatureUri should be added to FeatureProvider
                /*wfsRequest -> {
                    if (featureTypeDataset != null)
                        featureTypeDataset.requestUrl = null;//wfsRequest.getAsUrl();
                }*/
        //).build();
        return getResponse(featureQuery, outputStream -> new FeatureTransformerHtml(new OutputStreamWriter(outputStream), isFeatureCollection, featureType.getName()
                                                                                                                                                          .equals("inspireadressen"), groupings, group, query, range, featureTypeDataset, service.getCrsTransformations()
                                                                                                                                                                                                                                                 .getDefaultTransformer(), service.getSparqlAdapter(), service.getCodelistStore(), mustacheRenderer))
                .build();
    }

    private Response.ResponseBuilder getResponse(final FeatureQuery featureQuery, final FeatureTransformerBuilder featureTransformer) {
        return getResponse(featureQuery, featureTransformer, false);
    }

    private Response.ResponseBuilder getResponse(final FeatureQuery featureQuery, final FeatureTransformerBuilder featureTransformer, final boolean hitsOnly) {
        return getResponse(featureQuery, featureTransformer, null, hitsOnly);
    }

    private Response.ResponseBuilder getResponse(final FeatureQuery featureQuery, final FeatureTransformerBuilder featureTransformer, final String callback, final boolean hitsOnly) {
        Response.ResponseBuilder response = Response.ok();

        StreamingOutput stream;
        /*if (callback != null) {
            stream = new JSONPStreamingOutput(callback) {
                @Override
                public void writeCallback(JSONPOutputStream os) throws IOException, WebApplicationException {
                    //WFSRequest request = new WFSRequest(service.getWfsAdapter(), featureQuery);
                    Optional<ListenableFuture<HttpEntity>> features;
                    if (hitsOnly) {
                        features = service.getFeatureProvider()
                                          .getFeatureCount(featureQuery);
                    } else {
                        features = service.getFeatureProvider()
                                          .getFeatureStream(featureQuery);
                    }
                    if (!features.isPresent()) {
                        throw new IllegalStateException("No features available for type");
                    }

                    try {
                        GMLAnalyzer analyzer;
                        //if (range != null) {
                        //    analyzer = new MultiGMLAnalyzer(gmlAnalyzer.with(output), new RangeHeader.RangeWriter(response, range));
                        //} else {
                        analyzer = gmlAnalyzer.with(os);
                        //}

                        GMLParser gmlParser = new GMLParser(analyzer, service.staxFactory);
                        if (featureType.getMappings()
                                       .isEmpty()) {
                            gmlParser.enableTextParsing();
                        }
                        gmlParser.parse(features.get(), featureType.getNamespace(), featureType.getName());

                    } catch (ExecutionException ex) {
                        // ignore
                    }
                }
            };
        } else {*/
        //TODO: start parsing before this line, in StreamingOutput call Analyzer.startWrite(output)
        stream = outputStream -> {

            //WFSRequest request = new WFSRequest(service.getWfsAdapter(), featureQuery);
                /*Optional<ListenableFuture<HttpEntity>> features;
                if (hitsOnly) {
                    features = service.getFeatureProvider()
                                      .getFeatureCount(featureQuery);
                } else {
                    features = service.getFeatureProvider()
                                      .getFeatureStream(featureQuery);
                }

                if (!features.isPresent()) {
                    throw new IllegalStateException("No features available for type");
                }

                try {
                    GMLAnalyzer analyzer;
                    //if (range != null) {
                    //    analyzer = new MultiGMLAnalyzer(gmlAnalyzer.with(output), new RangeHeader.RangeWriter(response, range));
                    //} else {
                    analyzer = gmlAnalyzer.with(output);
                    //}

                    GMLParser gmlParser = new GMLParser(analyzer, service.staxFactory);
                    if (featureType.getMappings()
                                   .isEmpty()) {
                        gmlParser.enableTextParsing();
                    }
                    gmlParser.parse(features.get(), featureType.getNamespace(), featureType.getName());

                } catch (ExecutionException ex) {
                    // ignore
                }*/

            FeatureStream<FeatureTransformer> featureTransformStream = service.getFeatureProvider()
                                                                              .getFeatureTransformStream(featureQuery, akkaHttp);

            featureTransformStream.apply(featureTransformer.with(outputStream))
                                  .exceptionally(throwable -> {
                                      throw new IllegalStateException("No features available", throwable);
                                  })
                                  .toCompletableFuture()
                                  .join();
        };
        //}

        return response.entity(stream)
                       .header("Access-Control-Allow-Origin", "*")
                       .header("Access-Control-Allow-Methods", "GET");
    }

    private Response getWfsResponse(final FeatureQuery featureQuery, GmlConsumerBuilder gmlConsumer) {
        StreamingOutput stream = outputStream -> {
            /*Optional<ListenableFuture<HttpEntity>> features = service.getFeatureProvider()
                                                                     .getFeatureStream(featureQuery);
            if (!features.isPresent()) {
                throw new IllegalStateException("No features available for type");
            }

            try {
                features.get()
                        .get()
                        .writeTo(output);
            } catch (InterruptedException | ExecutionException e) {
                throw new IllegalStateException("No features available");
            }*/

            FeatureStream<GmlConsumer> featureTransformStream = service.getFeatureProvider()
                                                                       .getFeatureStream(featureQuery, akkaHttp);

            featureTransformStream.apply(gmlConsumer.with(outputStream))
                                  .exceptionally(throwable -> {
                                      throw new IllegalStateException("No features available", throwable);
                                  })
                                  .toCompletableFuture()
                                  .join();
        };


        return Response.ok()
                       .entity(stream)
                       .type("application/gml+xml;version=3.2")
                       .build();
    }

    private FeatureTypeConfiguration getFeatureTypeForLayerId(String id) {
        Optional<FeatureTypeConfiguration> featureType = service.getFeatureTypeByName(id);

        if (!featureType.isPresent() || (service.getServiceProperties()
                                                .getMappingStatus()
                                                .isEnabled() && !featureType.get()
                                                                            .isEnabled())) {
            throw new ResourceNotFound();
        }

        return featureType.get();
    }
}

